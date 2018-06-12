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

package de.infsec.tpl.profile;

import java.io.Serializable;
import java.util.List;

import com.ibm.wala.ipa.cha.IClassHierarchy;

import de.infsec.tpl.hash.HashTree;
import de.infsec.tpl.pkg.PackageTree;


public class AppProfile extends Profile implements Serializable {

	private static final long serialVersionUID = -1667876249936640164L;

	public AppProfile(PackageTree pTree, List<HashTree> hashTrees) {
		super(pTree, hashTrees);
	}

	public static AppProfile create(IClassHierarchy cha) {
		// generate app package tree
		PackageTree ptree = Profile.generatePackageTree(cha);

		// generate app hash trees
		List<HashTree> hashTrees = Profile.generateHashTrees(cha);

		return new AppProfile(ptree, hashTrees);
	}
}
