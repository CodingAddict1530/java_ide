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

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import org.fxmisc.richtext.InlineCssTextArea;

/**
 * A custom InlineCssTextArea for the console.
 */
public class ConsoleTextArea extends InlineCssTextArea {

    /**
     * The length of the protected text.
     * Final array to be able to use it in lambdas.
     */
    private final int[] protectedTextLength = new int[] {0};

    /**
     * Whether changes to text should be ignored or not.
     */
    private boolean ignore = false;

    /**
     * ChangeListener to react to changes in text.
     */
    private final ChangeListener<String> textChangeListener;

    /**
     * Instantiates a new ConsoleTextArea.
     */
    public ConsoleTextArea() {

        super();
        this.textChangeListener = (observable, oldValue, newValue) -> {

            if (!ignore){
                int newLength = newValue.length();

                // Revert changes to the text.
                if (this.getCaretPosition() < protectedTextLength[0]) {
                    String oldProtectedText = oldValue.substring(0, protectedTextLength[0]);
                    try {
                        String newProtectedText = newValue.substring(0, protectedTextLength[0]);
                        if (!oldProtectedText.equals(newProtectedText)) {
                            this.replaceText(oldValue);
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        this.replaceText(oldValue);
                    }

                } else {
                    protectedTextLength[0] = newLength;
                }
            } else {
                ignore = false;
            }

        };
    }

    /**
     * Overrides initial behaviour to effectively replace the entire the text.
     *
     * @param text The new text.
     */
    @Override
    public void replaceText(String text) {

        // Enable ignore so new text doesnt get reverted.
        ignore = true;

        // Run changes to the TextArea on the JavaFX Thread.
        Platform.runLater(() -> {
            super.replaceText(text);
            this.moveTo(this.getLength());
            this.requestFollowCaret();
        });

    }

    /**
     * Enables the TextArea to reject changes to text.
     */
    public void protectText() {

        protectedTextLength[0] = this.getLength();
        this.textProperty().addListener(this.textChangeListener);
    }

    /**
     * Lets the text in the TextArea to be changed.
     */
    public void unprotectText() {

        this.textProperty().removeListener(this.textChangeListener);
    }

}
