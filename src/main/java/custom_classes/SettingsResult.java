package custom_classes;

import java.nio.file.Path;

public record SettingsResult(Path jdkFolder, Theme theme, int fontSize) {

}
