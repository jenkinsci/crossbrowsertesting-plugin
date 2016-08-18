#CrossBrowserTesting.com Jenkins Plugin
#### This plugin integrates Jenkins users with Selenium Testing and Screenshot Testing on CrossBrowserTesting.com. CrossBrowserTesting.com provides cross browser testing of websites, webpages, and web applications on Windows, Macs, and real iPhones, iPads, and Android Phones and Tablets.
Wiki: https://wiki.jenkins-ci.org/display/JENKINS/CrossBrowserTesting+Plugin

### Installation

##### [via the interface][jenkins_install_interface]
1. Go to your installation's management screen.
2. Click **Manage Plugins**.
3. Click the **Available** tab.
4. Find the **CrossBrowserTesting.com Plugin** and select the checkbox.
5. then click either **Install without restart** or **Download now and install after restart**.

##### [by hand][jenkins_install_byhand] (*not recommended*)
1. Download [CrossBrowserTesting.hpi][latest_version].
2. Save the downloaded .hpi file into your `$JENKINS_HOME/plugins` directory.
3. Restart Jenkins.

### Configuration

##### Environment Variables
The CrossBrowserTesting.com Jenkins Plugin passes your build step information to your Selenium scripts as environment variables. The exact syntax will vary depending on your scripting language.

| Variable | Description|
|----------|------------|
|CBT_USERNAME| the username used on CrossBrowserTesting.com for Selenium Testing |
|CBT_APIKEY| the apikey used on CrossBrowserTesting.com for Selenium Testing |
|CBT_BUILD_NAME| the Jenkins Project's name |
|CBT_BUILD_NUMBER| the Jenkins Project's current build number |
|CBT_OPERATING_SYSTEM| the apiname of the selected Operating System |
|CBT_BROWSER| the apiname of the selected Browser |
|CBT_RESOLUTION| the name of the selected Screen Resolution |

See the examples directory for a few language specific sample scripts.

##### Saving Your CrossBrowserTesting.com API Credentials
1. Go to your installation's management screen.
2. Click **Configure System**.
3. Find the section labeled **CrossBrowserTesting.com**.
4. Enter your CrossBrowserTesting.com Username and API Key information. You can find your api key [here][cbt_apidocs] (must be logged in)
5. Click **Save**.

##### Build/Configure
###### Version \>= 0.11 Build Environment
1. Configure your Jenkins Project.
2. In *Build Environment*, Check the box next to *CrossBrowserTesting.com*
3. For Screenshot Tests: Pick a BrowserList and enter a URL (you must have you API credentials saved for the browserlist to populate)
4. For Selenium Tests: Add the **Operating System**, **Browser**, and **Screen Resolution** that you want to run a Selenium Test on. *You can add multiple configurations by clicking **Add Selenium Tests** *
5. Click **Save**
6. Click **Build Now** to build the project. All files in the project's workspace will be ran (make sure the selenium scripts are executable). Output from the selenium script will be displayed in **Console Output** for the build.
7. Optional: Check **Use Local Tunnel** to run tests on webpages behind your firewall. (The [CBT NodeJS Tunnel] [nodejs_tunnel] must be installed globally.)

###### Version <= 0.10 Build Step
1. Configure your Jenkins Project.
2. Click **Add build step**.
3. Click **CrossBrowserTesting.com**.
4. Add the **Operating System**, **Browser**, and **Screen Resolution** that you want to run a Selenium Test on. *You can add multiple build steps to run multiple selenium tests.*
5. Click **Save**
6. Click **Build Now** to build the project. All files in the project's workspace will be ran (make sure the selenium scripts are executable). Output from the selenium script will be displayed in **Console Output** for the build.

### Building the plugin for testing/development

##### Requirements:
- [JDK][java] &#8805; 8
- [Maven][maven] &#8805; 3

<pre>mvn hpi:run</pre>
##### To release
Make sure the pom.xml file's version has the new point release and has **-SNAPSHOT**
<pre> mvn release:prepare release:perform </pre>

[cbt_apidocs]: https://crossbrowsertesting.com/apidocs/v3/
[latest_version]: https://repo.jenkins-ci.org/jenkinsci/releases/org/jenkins-ci/plugins/crossbrowsertesting/0.21/crossbrowsertesting-0.21.hpi
[maven]: https://maven.apache.org/index.html
[java]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[jenkins_install]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins
[jenkins_install_interface]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Usingtheinterface
[jenkins_install_byhand]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Byhand
[nodejs_tunnel]: https://github.com/crossbrowsertesting/cbt-tunnel-nodejs
