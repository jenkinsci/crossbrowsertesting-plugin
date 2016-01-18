##CrossBrowserTesting.com Jenkins Plugin
#### Integrates Jenkins with Selenium Testing CrossBrowserTesting.com
Wiki: https://wiki.jenkins-ci.org/display/JENKINS/CrossBrowserTesting+Plugin

#### Installation

###### [via the interface][jenkins_install_interface]
1. Go to your installation's management screen.
2. Click **Manage Plugins**.
3. Click the **Available** tab.
4. Find the **CrossBrowserTesting.com Plugin** and select the checkbox.
5. then click either **Install without restart** or **Download now and install after restart**.

###### [by hand][jenkins_install_byhand] (*not recommended*)
1. Download [CrossBrowserTesting.hpi][latest_version].
2. Save the downloaded .hpi file into your `$JENKINS_HOME/plugins` directory.
3. Restart Jenkins.

#### Configuration

###### Environment Variables
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

###### Saving Your CrossBrowserTesting.com API Credentials
1. Go to your installation's management screen.
2. Click **Configure System**.
3. Find the section labeled **CrossBrowserTesting.com**.
4. Enter your CrossBrowserTesting.com Username and API Key information.
you can find your api key [here][cbt_apidocs] (must be logged in)
5. Click **Save**.

###### Build Step
1. Configure your Jenkins Project.
2. Click **Add build step**.
3. Click **CrossBrowserTesting.com**.
4. Add the **Operating System**, **Browser**, and **Screen Resolution** that you want to run a Selenium Test on. *You can add multiple build steps to run multiple selenium tests.*
5. Click **Save**
6. Click **Build Now** to build the project. Output from the selenium script will 

#### Building the plugin for testing

###### Requirements:
- [JDK][java] &#8805; 8
- [Maven][maven] &#8805; 3

<pre>mvn hpi:run</pre>
###### To release
Make sure the pom.xml file's version has the new point release and has **-SNAPSHOT**
<pre> mvn release:prepare release:perform </pre>

[cbt_apidocs]: https://crossbrowsertesting.com/apidocs/v3/
[latest_version]: http://updates.jenkins-ci.org/latest/crossbrowsertesting.hpi
[maven]: https://maven.apache.org/index.html
[java]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[jenkins_install]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins
[jenkins_install_interface]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Usingtheinterface
[jenkins_install_byhand]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Byhand