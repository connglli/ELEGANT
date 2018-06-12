/*
 * Copyright (c) 2015-2017  Erik Derr [derr@cs.uni-saarland.de]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package de.infsec.tpl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import de.infsec.tpl.manifest.ProcessManifest;
import de.infsec.tpl.hash.Hash;
import de.infsec.tpl.hash.HashTree;
import de.infsec.tpl.hash.HashTree.Node;
import de.infsec.tpl.hash.HashTree.PackageNode;
import de.infsec.tpl.pkg.PackageTree;
import de.infsec.tpl.pkg.PackageUtils;
import de.infsec.tpl.pkg.PackageUtils.RELATIONSHIP;
import de.infsec.tpl.profile.AppProfile;
import de.infsec.tpl.profile.LibProfile;
import de.infsec.tpl.profile.ProfileMatch;
import de.infsec.tpl.profile.ProfileMatch.HTreeMatch;
import de.infsec.tpl.profile.ProfileMatch.MatchLevel;
import de.infsec.tpl.stats.AppStats;
import de.infsec.tpl.utils.*;


public class LibraryIdentifier {

    private List<String> androidJarsPathes;

	private IClassHierarchy cha;
	private Map<String,String> uniqueLibraries;   // unique library name -> highest version 
	
	private AppStats stats;

	public static Set<String> ambiguousRootPackages = new TreeSet<String>() {
		private static final long serialVersionUID = 7951760067476257884L;
	{
		add("com.google");              add("com.google.android");
		add("com.google.android.gms");  add("android.support");
	}};
	
	public LibraryIdentifier(File appFile, List<String> androidJarsPathes) {
		this.stats = new AppStats(appFile);
		this.androidJarsPathes = androidJarsPathes;
	}

	private void createClassHierarchy() throws IOException, ClassHierarchyException {
		long s = System.currentTimeMillis();

		// check if we have a multi-dex file
		stats.isMultiDex = ApkUtils.isMultiDexApk(stats.appFile);
		if (stats.isMultiDex)
            System.out.println("Multi-dex apk detected - Code is merged to single class hierarchy!");

		URI[] uris = (URI[]) androidJarsPathes.stream().map(f -> new File(f).toURI()).toArray();

        // create analysis scope and generate class hierarchy
		// we do not need additional libraries like support libraries,
		// as they are statically linked in the app code.
		final AnalysisScope scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(new File(stats.appFile.getAbsolutePath()).toURI(), null /* no exclusions */, null /* we always pass an android lib */, uris);

		cha = ClassHierarchy.make(scope);
	}

	public List<String> identifyLibraries(List<LibProfile> profiles) throws NoSuchAlgorithmException, ClassNotFoundException, IOException, ClassHierarchyException {
	    long starttime = System.currentTimeMillis();

		// parse AndroidManifest.xml 
		stats.manifest = parseManifest(stats.appFile);
		
		// check stat file <stats-dir>/package-level1/package-level2/appName_appVersionCode.data
		String statsFileName = stats.appFile.getName().replaceAll("\\.jar", "").replaceAll("\\.apk", "").replaceAll("\\.aar", "") + "_" + stats.manifest.getVersionCode();  // without file suffix
		
		List<String> ptoken = PackageUtils.parsePackage(stats.manifest.getPackageName());
		File statsSubDir = null;
		if (ptoken.size() > 0) {
			if (ptoken.size() > 1)
				statsSubDir = new File(ptoken.get(0) + File.separator + ptoken.get(1));
			else
				statsSubDir = new File(ptoken.get(0));
		}
		
		stats.profiles = profiles;
		uniqueLibraries = LibProfile.getUniqueLibraries(profiles);

		// create CHA
		createClassHierarchy();
		
		// generate app package tree and hash trees
		AppProfile appProfile = AppProfile.create(cha);
		stats.pTree = appProfile.packageTree;
		stats.appHashTrees = appProfile.hashTrees;

		// fast scan (heuristic) - check if lib root package is in app
		stats.packageMatches = new HashSet<>();
		for (LibProfile profile: profiles) {
			// check if library root package is present in app (for validation purposes)
			String rootPackage = profile.packageTree.getRootPackage();
			
			// In some edge case the automatic root package extraction gives us a generic package that could match multiple different libraries.
			// In these cases it is better to ignore them instead of getting a lot of false matches
			if (rootPackage == null || ambiguousRootPackages.contains(rootPackage)) continue;
			
			boolean match = appProfile.packageTree.containsPackage(rootPackage);
			if (match) { stats.packageMatches.add(profile.description.name); }
		}
		
		// check app against all loaded profiles (exact + partial matching)
		List<ProfileMatch> results = new ArrayList<ProfileMatch>();

		for (LibProfile profile: profiles) {
			// check if this is the most current library version
			profile.setIsDeprecatedLib(!uniqueLibraries.get(profile.description.name).equals(profile.description.version));
			
			// compute similarity scores for each hash tree
			ProfileMatch pm = partialMatchForTrees(cha, appProfile, profile, MatchLevel.CLASS);
			results.add(pm);
		}

		stats.pMatches = results;
        stats.processingTime = System.currentTimeMillis() - starttime;

        return collectResults(results);
	}

	/**
	 * Compute similarity scores for all provided {@link HashTree}. 
	 * @param cha the {@link IClassHierarchy}
	 * @param appProfile  the {@link AppProfile}
	 * @param libProfile  the {@link LibProfile}
	 * @param lvl  the level of matching to be applied (currently either Package or Class level)
	 * @return  a {@link ProfileMatch} for the provided library profile
	 * @throws NoSuchAlgorithmException
	 */
	public ProfileMatch partialMatchForTrees(final IClassHierarchy cha, final AppProfile appProfile, final LibProfile libProfile, final MatchLevel lvl) throws NoSuchAlgorithmException {
		ProfileMatch pMatch = new ProfileMatch(libProfile);

		// check if library root package is present in app (for validation purposes)
		String rootPackage = libProfile.packageTree.getRootPackage();
		pMatch.libRootPackagePresent = rootPackage == null? false : appProfile.packageTree.containsPackage(rootPackage);

		// calculate scores for each hash tree
		for (HashTree appHashTree: appProfile.hashTrees) {
			partialMatch(cha, pMatch, appHashTree, appProfile.packageTree, libProfile, lvl);
		}

		return pMatch;
	}

	/**
	 * Multi-step approach to compute the similarity score between a given library and an application.
	 * We first check for a full match by comparing the package hashes of the library with the ones from the application.
	 * If there is no full match we compute a candidate list for each app package, compute partitions (potential root packages) and
	 * determine the maximum over all partitions.
	 * 
	 * @param cha  the {@IClassHierarchy}
	 * @param pMatch   a {@ProfileMatch} in which the result is stored as side-effect
	 * @param appHashTree  the generated {@link HashTree}
	 * @param appTree  the application {@link PackageTree}
	 * @param lib  the {@link LibProfile} to match against
	 * @param lvl  the level of matching to be applied (currently either Package or Class level)
	 * @throws NoSuchAlgorithmException
	 */
	public void partialMatch(final IClassHierarchy cha, final ProfileMatch pMatch, final HashTree appHashTree, final PackageTree appTree, final LibProfile lib, final MatchLevel lvl) throws NoSuchAlgorithmException {
		// retrieve hash tree with same config from profile
		HashTree libHashTree = HashTree.getTreeByConfig(pMatch.lib.hashTrees, appHashTree.getConfig());
		HTreeMatch match = pMatch.createResult(appHashTree.getConfig());

		if (libHashTree == null) {
			System.err.println("Could not find lib hash tree for config: " + appHashTree.getConfig());
			return;
		}

		/*
		 *  step 0. shortcut - check if library fully matches by comparing the package hashes
		 */
		if (appHashTree.getPackageNodes().containsAll(libHashTree.getPackageNodes())) {
			// update results
			match.simScore = ProfileMatch.MATCH_HTREE_FULL;
			
			List<Node> matchingNodes = new ArrayList<Node>(appHashTree.getPackageNodes());
			matchingNodes.retainAll(libHashTree.getPackageNodes());
			match.matchingNodes = HashTree.toPackageNode(matchingNodes);
			
			pMatch.addResult(match);
			return;
		}
		
		// we do not perform partial matching for libs that have multiple lib root packages
		if (lib.packageTree.getRootPackage() == null) {
			match.simScore = ProfileMatch.MATCH_HTREE_NO_ROOT_PCKG;
			pMatch.addResult(match);
			return;

		}

		/*
		 *  step 1. compute candidate list
		 *
		 *  Candidate list of each library package sorted by similarity score, i.e.
		 *    lp1 ∶ ap1 (0.95), ap2 (0.84), ap3 (0.75)
		 *    lp3 ∶ ap6 (0.91), ap4 (0.60)
		 *    lp2 ∶ ap7 (0.85), ap9 (0.82)
		 */
		long time = System.currentTimeMillis();
		HashMap<Node, List<Pair<Node, Float>>> candidateList = new HashMap<Node, List<Pair<Node, Float>>>();
		for (Node lp: libHashTree.getPackageNodes()) {
			ArrayList<Pair<Node, Float>> clist = new ArrayList<Pair<Node, Float>>();  // candidate list for lp

			for (Node ap: appHashTree.getPackageNodes()) {
				// filter application packages that start with declared manifest app package name
				// TODO: unfortunately most app packages do only partially match the manifest package name. This means to match more app packages
				//       we would have to test partially (but: this could lead to false positives if we have libs from the same developer)
				if (((PackageNode) ap).packageName.startsWith(stats.manifest.getPackageName()))
					continue;
				
				float score = calcNodeSimScore(lp, ap);
			
				if (score > ProfileMatch.MIN_CLAZZ_SCORE) {
					// update candidate list
					clist.add(new Pair<>(ap, score));
				}
			}

			Collections.sort(clist, new SimScoreComparator());   // sort candidate list by simScore
			candidateList.put(lp, clist);
		}
		
		// sort tree map by highest candidate values
		TreeSet<Map.Entry<Node, List<Pair<Node, Float>>>> sortedCList = new TreeSet<Map.Entry<Node, List<Pair<Node, Float>>>>(new CandidateListComparator());
		sortedCList.addAll(candidateList.entrySet());
		
		/*
		 *  step 2. get potential root packages -- partitions  (skip package name of apk)
		 *  Note: This will _not_ work if app developer manually renamed the library packages (e.g. prefixed lib packages with app package)  
		 */
		time = System.currentTimeMillis();
		String libraryRootPackage = pMatch.lib.packageTree.getRootPackage();
		int libPDepth = PackageUtils.packageDepth(libraryRootPackage);
		
		// retrieve potential app root packages of depth libPDepth
		Set<String> appRootPackages = new TreeSet<String>();
		for (Node ap: appHashTree.getPackageNodes()) {
			PackageNode apn = (PackageNode) ap;
			String pRootPackage = PackageUtils.getSubPackageOfDepth(apn.packageName, libPDepth);
			if (pRootPackage != null)
				appRootPackages.add(pRootPackage);
		}

		appRootPackages = getPartitionsByRootPackage(appRootPackages, lib.packageTree.getRootPackage());

		/*
		 *  step 3. compute maximum score for each partition (including selected app packages)
		 *          a score is a mapping of root package -> pair < score, nodes with individual package scores >
		 */
		// pre-compute library package relationships
		// TODO: could be computed even earlier (pmatchForTrees)
		List<RELATIONSHIP> libPackageRel = computePackageRelationships(sortedCList);

		HashMap<String, Pair<Float, List<Pair<Node, Float>>>> scores = new HashMap<String, Pair<Float, List<Pair<Node, Float>>>>();
		for (String rootPackage: appRootPackages) {
			time = System.currentTimeMillis();
			Pair<Float, List<Pair<Node, Float>>> partSimScore = calcPartitionSimScore(rootPackage, sortedCList, libPackageRel);
			if (partSimScore != null) {
				scores.put(rootPackage, partSimScore);
			}
		}

		/*
		 *  step 4. chose overall maximum (with a minimum threshold of @ProfileMatch.MIN_PARTIAL_MATCHING_SCORE)
		 */
		if (scores.isEmpty()) {
			// update results
			match.simScore = ProfileMatch.MATCH_HTREE_NONE;
			pMatch.addResult(match);
			
		} else {
			// get best score over partitions
			String rootPckg = "";
			float highScore = 0f;
			for (String root: scores.keySet()) {
				if (scores.get(root).first() > highScore) {
					highScore = scores.get(root).first();
					rootPckg = root;
				}
			}
			
			match.rootPackage = rootPckg;
			match.simScore = highScore;
		
			List<Node> matchingNodes = new ArrayList<Node>();
			for (Pair<Node, Float> p: scores.get(rootPckg).second()) {
				if (p != null)
					matchingNodes.add(p.first());
			}
	
			match.matchingNodes = HashTree.toPackageNode(matchingNodes);
			pMatch.addResult(match);
		}
	}

    // TODO: cumbersome
	private static List<RELATIONSHIP> computePackageRelationships(TreeSet<Map.Entry<Node, List<Pair<Node, Float>>>> candidateList) {
		// retrieve ordered list of library packages from the current candidate list
		ArrayList<PackageNode> libraryPackageNodes = new ArrayList<PackageNode>();
		for (Map.Entry<Node, List<Pair<Node, Float>>> entry: candidateList) {
			libraryPackageNodes.add((PackageNode) entry.getKey());
		}

		// compute package relationship for adjacent packages (used as additional app package filter in getCombinations)
		ArrayList<RELATIONSHIP> result = new ArrayList<RELATIONSHIP>();
		for (int i = 0; i < libraryPackageNodes.size()-1; i++) {
			result.add(PackageUtils.testRelationship(libraryPackageNodes.get(i).packageName, libraryPackageNodes.get(i+1).packageName));
		}
		
		return result;
	}
	
	private Pair<Float, List<Pair<Node, Float>>> calcPartitionSimScore(final String rootPackage, final TreeSet<Map.Entry<Node, List<Pair<Node, Float>>>> candidateList, final List<RELATIONSHIP> libPackageRel) {
		// create view on candidate list (filter app packages that do not start with rootPackage and app packages
		// that have a different depth as the lib package)
		// TODO: maybe could be improved by using a bitmask
		ArrayList<List<Pair<Node, Float>>> cList = new ArrayList<List<Pair<Node, Float>>>();

		int packagesWithCandidates = 0;
		for (Iterator<Map.Entry<Node, List<Pair<Node, Float>>>> it = candidateList.iterator(); it.hasNext(); ) {
			Map.Entry<Node, List<Pair<Node, Float>>> pckgCandidates = it.next();
			
			ArrayList<Pair<Node,Float>> filteredCandidates = new ArrayList<Pair<Node,Float>>();
			for (Pair<Node, Float> candidate: pckgCandidates.getValue()) {
				PackageNode pn = (PackageNode) candidate.first();
				
				// check if candidate package starts with root package and has the same package depth
				if (pn.packageName.startsWith(rootPackage) && PackageUtils.packageDepth(((PackageNode) pckgCandidates.getKey()).packageName) == PackageUtils.packageDepth(pn.packageName))
					filteredCandidates.add(candidate);
			}

			if (!filteredCandidates.isEmpty()) {
				packagesWithCandidates++;
			}
			
			cList.add(filteredCandidates);
		}

		// stop if less than half of lib packages have no candidate
		if ((((float) packagesWithCandidates / (float) candidateList.size()) < 0.5f)) {
			return null;
		}
		
		// test all combinations and retrieve maximum
		return getBestMatch(cList, libPackageRel);
	}
	
	/**
	 * Given a candidate list of app packages for every lib package, compute the optimal solution
	 * while preserving the package relationship.
	 * @param cList ordered candidate list
	 * @param libPackageRel  pre-computed relationship between two consecutive library packages
	 * @return  the optimal solution as similarity score and corresponding list of packages
	 */
	public static Pair<Float, List<Pair<Node, Float>>> getBestMatch(final ArrayList<List<Pair<Node, Float>>> cList, final List<RELATIONSHIP> libPackageRel) {
	    // keep track of the size of candidate arrays for each lib
		// Example cList: 
		//    lp1 ∶ ap1 (0.95), ap2 (0.84), ap3 (0.75)
		//    lp3 ∶ ap6 (0.91), ap4 (0.60)
		//    lp2 ∶ ap7 (0.85), ap9 (0.82)
		// => size array [3,2,2]
	    int sizeArray[] = new int[cList.size()];

	    // keep track of the index of each inner String array which will be used
	    // to make the next combination (access pattern)
	    int counterArray[] = new int[cList.size()];

	    // Discover the size of each inner array and populate sizeArray.
	    // Calculate the total number of combinations possible, here: 3*2*2 = 12
	    int totalCombinationCount = 1;
	    for(int i = 0; i < cList.size(); ++i) {
	        sizeArray[i] = cList.get(i).size();
	        totalCombinationCount *= cList.get(i).isEmpty()? 1 : cList.get(i).size();  // tolerate empty candidate list
	    }

	    // stop if we have too much combinations
		if (totalCombinationCount > 65536) {
			return null;
		}
	    
	    // only consider solutions that are better than the min matching score
		float highScore = ProfileMatch.MIN_PARTIAL_MATCHING_SCORE;
		List<Pair<Node, Float>> bestMatch = null;
		
		// test all combinations
		List<Pair<Node, Float>> curSolution = null;
	    for (int countdown = totalCombinationCount; countdown > 0; --countdown) {

	    	// calculate sim score for current combination (set in counterArray)
	    	float simScore = 0f;	    	
	        for(int i = 0; i < cList.size(); ++i) {
	        	List<Pair<Node,Float>> candidates = cList.get(i);
	        	simScore += candidates.isEmpty()? 0f : candidates.get(counterArray[i]).second();
	        }

			simScore = simScore / (float) cList.size();

			// if we have a new highscore, perform structural matching (package relationships) to
			// verify correctness of the solution
			if (simScore > highScore) {
				curSolution = new ArrayList<Pair<Node, Float>>();
				
		        for (int i = 0; i < cList.size()-1; i++) {
		        	if (cList.get(i).isEmpty() || cList.get(i+1).isEmpty())  // tolerate empty candidates
		        		continue;
		        	
		        	RELATIONSHIP candidateRel = PackageUtils.testRelationship(((PackageNode) cList.get(i).get(counterArray[i]).first()).packageName,
		        															  ((PackageNode) cList.get(i+1).get(counterArray[i+1]).first()).packageName);
		        	if (!libPackageRel.get(i).equals(candidateRel)) {
		        		curSolution = null;
		        		break;
		        	} else {		        	
		        		curSolution.add(cList.get(i).get(counterArray[i]));
		        	}
		        }
		        
		        if (curSolution != null) {
		        	highScore = simScore;
		        	bestMatch = new ArrayList<Pair<Node, Float>>(curSolution);
		        	if (!cList.get(cList.size()-1).isEmpty())
		        		bestMatch.add(cList.get(cList.size()-1).get(counterArray[cList.size()-1]));  // add last element (if existing)
		        }
		        
			}

			// Increment the counterArray so that the next combination is taken on the next iteration of this loop.
	        for (int incIndex = cList.size()-1; incIndex >= 0; --incIndex) {
	            if (counterArray[incIndex] + 1 < sizeArray[incIndex]) {
	                ++counterArray[incIndex];
	                // None of the indices of higher significance need to be
	                // incremented, so jump out of this for loop at this point.
	                break;
	            }
	            // The index at this position is at its max value, so zero it
	            // and continue this loop to increment the index which is more
	            // significant than this one.
	            counterArray[incIndex] = 0;
	        }
	    }

	    return bestMatch == null? null : new Pair<Float, List<Pair<Node, Float>>>(highScore, bestMatch);
	}
	
	// Filter candidates by lib root package
	private static Set<String> getPartitionsByRootPackage(Collection<String> partitions, String libRootPackage) {
		if (partitions.isEmpty() || partitions.size() == 1 || libRootPackage == null) 
			return new HashSet<String>(partitions);

		Set<String> result = new TreeSet<String>();
		for (String partition: partitions) {
			if (partition.startsWith(libRootPackage))
				result.add(partition);
		}

		return result.isEmpty()? new TreeSet<String>(partitions) : result;
	}
	
	
	private static List<Integer> intArray2List(int[] a) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i: a) list.add(i);
		return list;
	}

	/**
	 * Computes the similarity score between two nodes based on matching child nodes.
	 * @param libNode
	 * @param appNode
	 * @return  zero, if type of both nodes does not match (e.g. PackageNode/ClassNode)
	 *          one, if both node hashes match (per definition all childs match then as well)
	 *          a similarity score between 0..1 otherwise 
	 */
	public static float calcNodeSimScore(final Node libNode, final Node appNode) {
		// if they have not the exact same class e.g. PackageNode vs ClassNode, simScore is 0f
		if (!(libNode.getClass().equals(appNode.getClass())))
			return 0f;
		
		// if hashes are equal return 1f
		if (Hash.equals(libNode.hash, appNode.hash))
			return 1f;
		
		// calculate partial score
		List<Node> matchedNodes = new ArrayList<Node>();
		for (Node lNode: libNode.childs) {
			if (appNode.childs.contains(lNode)) {
				matchedNodes.add(lNode);
			}
		}
	
		return (float) matchedNodes.size() / (float) libNode.numberOfChilds();
	}

	/**
	 * Verbose human-readable log file report using pre-computed results
	 * @param results  pre-computed results for each hashtree
	 */
	
	// TODO: if debug, report any match with simScore > .25
	public List<String> collectResults(final List<ProfileMatch> results) {
	    List<String> prefixes = new ArrayList<>();

	    // Step1: print libs for which all configs match
		Set<String> exactMatches = new TreeSet<>();
		for (String libName: uniqueLibraries.keySet()) {
			for (ProfileMatch pm: results) {
				if (pm.lib.description.name.equals(libName) && pm.doAllConfigsMatch()) {
					exactMatches.add(pm.lib.packageTree.getRootPackage());
				}
			}
		}

		// Step2: print all libs which have at least one matching config but do not match all configs
		Set<String> almostExactMatches = new TreeSet<>();
		for (String libName: uniqueLibraries.keySet()) {
			if (exactMatches.contains(libName)) continue;
			
			// if the same lib matches in different versions and different number of exact matches, show only the best matches
			List<ProfileMatch> bestMatches = new ArrayList<ProfileMatch>();
			for (ProfileMatch pm: results) {
				if (pm.lib.description.name.equals(libName) && pm.isMatch()) {
					if (bestMatches.isEmpty() || bestMatches.get(0).getMatchedConfigs().size() == pm.getMatchedConfigs().size())
						bestMatches.add(pm);
					else if (bestMatches.get(0).getMatchedConfigs().size() < pm.getMatchedConfigs().size()) {
						bestMatches.clear();
						bestMatches.add(pm);
					}
				}
			}
			
			for (ProfileMatch pm: bestMatches) {
				almostExactMatches.add(pm.lib.packageTree.getRootPackage());       /// TODO TODO : full code match?  (if not how much code?)
			}
		}
		
		// unify all libraries for which we have at least one matching config
		exactMatches.addAll(almostExactMatches);

		List<String> partialMatches = new ArrayList<>();
        for (String lib: uniqueLibraries.keySet()) {
            if (!exactMatches.contains(lib)) {

                // print only highest match for a given library (can be multiple libs)
                List<ProfileMatch> pMatches = new ArrayList<>();
                float highScore = ProfileMatch.MATCH_HTREE_NONE;

                for (ProfileMatch pm: results) {
                    if (pm.lib.description.name.equals(lib)) {
                        if (pm.getHighestSimScore().simScore >= highScore && pm.getHighestSimScore().simScore > ProfileMatch.MATCH_HTREE_NONE) {
                            pMatches = new ArrayList<>();
                            pMatches.add(pm);
                            highScore = pm.getHighestSimScore().simScore;
                        }
                    }
                }

                for (ProfileMatch pm: pMatches) {
                    partialMatches.add(pm.lib.packageTree.getRootPackage());
                }
            }
        }

        prefixes.addAll(exactMatches);
        prefixes.addAll(partialMatches);

        return prefixes;
	}

	private ProcessManifest parseManifest(File appFile) {
		ProcessManifest pm = new ProcessManifest();
		pm.loadManifestFile(appFile.getAbsolutePath());
		return pm;
	}

	/*
	 * Compares Collections with Pair<Node, Float> according the float value (descending)
	 */
	private class SimScoreComparator implements Comparator<Pair<Node, Float>> {
		
		@Override
		public int compare(Pair<Node, Float> p1, Pair<Node, Float> p2) {
			return p2.second().compareTo(p1.second());
		}
	}
	
	private class CandidateListComparator implements Comparator<Map.Entry<Node,List<Pair<Node, Float>>>> {

		@Override
		public int compare(Map.Entry<Node, List<Pair<Node, Float>>> entry1, Map.Entry<Node, List<Pair<Node, Float>>> entry2) {
			if (entry1.getValue().isEmpty())
				return 1;
			else if (entry2.getValue().isEmpty())
				return -1;
			else {
				int val = entry2.getValue().get(0).second().compareTo(entry1.getValue().get(0).second());  // candidate lists are already ordered, hence it suffices to compare first value of each list
				return val == 0? ((PackageNode) entry1.getValue().get(0).first()).packageName.compareTo(((PackageNode) entry2.getValue().get(0).first()).packageName) : val;
			}
		}
	}

}
