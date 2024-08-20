
# FUSION IDE

A loaded Java Integrated Development Environment that provides Syntax analysis, Project management, Debugging and so on...

Built with JavaFX and Gradle.

## Copy the repository

1. Clone the repository:

```bash
  git clone https://github.com/CodingAddict1530/java_ide.git
```
2. Navigate to the project:

```bash
  cd my-project
```
3. Ensure you have gradle installed:
- You need JDK 17 or later. Install it from [Oracle's website](https://www.oracle.com/java/technologies/downloads/?er=221886) or use a distribution like [Amazon Corretto](https://docs.aws.amazon.com/corretto/).
- Gradle 8.x or later is required. You can install it from [Gradle's website](https://gradle.org/) or use the Gradle Wrapper included in the project.
4. Build the project:
If you are using gradle wrapper:
```bash
./gradlew build
```
Alternatively, if you have Gradle installed globally:
```bash
gradle build
```
5. Run the IDE:
The IDE you are using may as well provide a way to run the project.

Or else, you can run the IDE using:
```bash
./gradlew run
```
6. The code is optimized to run as an executable, and so it expects to have jdk to run gradle and the language server. 
To let the code function fully, copy your jdk to the root directory and rename it to `dont_snoop`.
## Download
Alternatively, you can download a setup if you wish to use the application.
1. **Visit the Releases Page**: Go to the [Releases](https://github.com/your-username/your-repo/releases) page of this repository.
   
2. **Find the Latest Release**: Look for the most recent release, which is tagged with the latest version number (e.g., `v1.0.0`).

3. **Download the Executable**:
   - Click on the release version you want to download.
   - Under the "Assets" section, you will find the downloadable files. Click on the file (e.g., `your-software-v1.0.0.exe`) to start the download.

4. **Run the setup**:
   - Double-click on the executable and follow the steps.
   - Once it is done, if you don't have a java installation already
   - Add the folder that will be in the installation folder named `dont_snoop`.

5. **Adding to PATH (Optional)**:
   1. Open the System Properties:
      - Press Win + X and select System.
      - Click on Advanced system settings on the left.
      - In the System Properties window, click the Environment Variables button.
   2. Edit the PATH Variable:
      - In the Environment Variables window, locate the System variables section.
      - Scroll down and select the Path variable, then click Edit.
   3. Add a New Path Entry:
      - In the Edit Environment Variable window, click New and `'path-to-fusion'\dont_snoop\bin`.
      - Click OK to close the Edit Environment Variable window.
   4. Apply Changes:
      - Click OK to close the Environment Variables window.
      - Click OK again to close the System Properties window.
   5. Verify the Change:
      - Open a new Command Prompt and run:
   ```bash
   echo %PATH%
   ```
      - Verify that your new path entry is listed.

## Usage/Examples

- When you start the application, it will index all possible classes in the background. This will take some minutes but won't halt the application.

- Use the menus to interact with different components such as creating files, projects etc.
![Screenshot](images/Fusion%20IDE%202024-08-20%206_56_27%20PM.png)

- Create a new project. Gradle will initialize the project in a few seconds. Then create files and start working!
![Screenshot](images/Fusion%20IDE%202024-08-20%206_57_03%20PM.png)
![Screenshot](images/Fusion%20IDE%202024-08-20%206_57_40%20PM.png)
![Screenshot](images/Fusion%20IDE%202024-08-20%206_58_19%20PM.png)
![Screenshot](images/Fusion%20IDE%202024-08-20%207_02_56%20PM.png)





## Features

- Integrated Java editor with syntax highlighting.
- Project management and build tools (gradle).
- Modern user interface with JavaFX.
- Real-time code analysis and error highlighting and warnings.
- Code execution.
- Debugging features such as single stepping.
- Terminal (COMING SOON...).


## Contributing

Contributions are always welcome!

1. **Fork the Repository**: Click on the "Fork" button at the top right of the repository page.
2. **Create a Feature Branch**:
```bash
git checkout -b feature/YourFeatureName
```
3. **Make Your Changes**: Implement the new feature or fix the issue.
4. **Commit Your Changes**:
```bash
git commit -am 'Add some feature'
```
5. **Push to the Branch**:
```bash
git push origin feature/YourFeatureName
```
6. **Create a Pull Request**: Go to the repository on GitHub and create a pull request from your feature branch to the `master` branch.


## License

[APACHE LICENSE 2.0](https://www.apache.org/licenses/LICENSE-2.0)


## Contact

For any questions or feedback, you can reach out to:
- **Alexis Mugisha** - amugisha005@gmail.com