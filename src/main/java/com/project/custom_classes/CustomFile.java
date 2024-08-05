/*
 * Copyright 2024 Alexis Mugisha
 * https://github.com/CodingAddict1530
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.project.custom_classes;

import java.io.File;

/**
 * A custom File class that holds its package as well.
 */
public class CustomFile extends File {

    /**
     * The name of the package of the file.
     * It is relative to src/main/java.
     */
    private String packageName;

    /**
     * Instantiates a CustomFile object.
     *
     * @param path Path object representing the path to the file.
     * @param packageName The package this file belongs to.
     */
    public CustomFile(String path, String packageName) {

        super(path);
        this.packageName = packageName;
    }

    /**
     * Instantiates a CustomFile object.
     *
     * @param parent The parent directory of the file.
     * @param child The name of the file.
     * @param packageName The package this file belongs to.
     */
    public CustomFile(String parent, String child, String packageName) {

        super(parent, child);
        this.packageName = packageName;
    }

    /**
     * Instantiates a CustomFile object.
     *
     * @param parent The parent directory of the file.
     * @param child The name of the file.
     * @param packageName The package this file belongs to.
     */
    public CustomFile(CustomFile parent, String child, String packageName) {

        super(parent, child);
        this.packageName = packageName;
    }

    /**
     * Retrieves the package name of the file.
     *
     * @return The package.
     */
    public String getPackageName() {

        return this.packageName;
    }

    /**
     * Updates the package name of the file.
     *
     * @param packageName The new package name.
     */
    public void setPackageName(String packageName) {

        this.packageName = packageName;
    }
}
