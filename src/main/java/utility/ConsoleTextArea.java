package utility;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import org.fxmisc.richtext.InlineCssTextArea;

public class ConsoleTextArea extends InlineCssTextArea {

    private final int[] protectedTextLength = new int[] {0};
    private boolean ignore = false;
    private final ChangeListener<String> textChangeListener;

    public ConsoleTextArea() {

        super();
        this.textChangeListener = (observable, oldValue, newValue) -> {

            if (!ignore){
                int newLength = newValue.length();

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
                    //System.out.println(this.getCaretPosition());
                }
            } else {
                ignore = false;
            }

        };
    }

    @Override
    public void replaceText(String text) {

        ignore = true;
        super.replaceText(text);
        Platform.runLater(() -> {
            this.moveTo(this.getLength());
            this.requestFollowCaret();
        });
    }

    public void protectText() {

        protectedTextLength[0] = this.getLength();
        this.textProperty().addListener(this.textChangeListener);
    }

    public void unprotectText() {

        this.textProperty().removeListener(this.textChangeListener);
    }

}
