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

### Building the plugin for testing/development

##### Requirements:
- [JDK][java] &#8805; 8

<pre>gradlew server</pre>
###### to specify the version of jenkins:
<pre>gradlew server -Pjenkins=${jenkins_version}</pre>

###### to make it run a little faster, turn on caching:
<pre>gradlew server -Pjenkins=${jenkins_version} -Dstapler.jelly.noCache=false</pre>
##### To release:
make sure all code has been committed to your local git repo
<pre>gradlew publish release</pre>
credentials for the publish should be in a file [~/.jenkins-ci.org][jenkins_credentials_documentation]

[jenkins_credentials_documentation]: https://wiki.jenkins.io/display/JENKINS/Dot+Jenkins+Ci+Dot+Org
[latest_version]: http://updates.jenkins-ci.org/latest/crossbrowsertesting.hpi
[maven]: https://maven.apache.org/index.html
[java]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[jenkins_install]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Howtoinstallplugins
[jenkins_install_interface]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Usingtheinterface
[jenkins_install_byhand]: https://wiki.jenkins-ci.org/display/JENKINS/Plugins#Plugins-Byhand
