#!/usr/local/bin/python
# Please visit http://selenium-python.readthedocs.org/en/latest/index.html for detailed installation and instructions

import unittest
import os
from selenium import webdriver

caps = {}
username = os.environ.get("CBT_USERNAME")
key = os.environ.get("CBT_APIKEY")

caps['name'] = os.environ.get("CBT_BUILD_NAME")    # Need to set these to something else?
caps['build'] = os.environ.get("CBT_BUILD_NUMBER") # Check below to learn how
caps['browser_api_name'] = os.environ.get("CBT_BROWSER")
caps['os_api_name'] = os.environ.get("CBT_OPERATING_SYSTEM")
caps['screen_resolution'] = os.environ.get("CBT_RESOLUTION")
caps['record_video'] = 'true'

# Need to set the build and name capabilites to something else?
# Just set the jenkins_name and jenkins_build caps to the proper environment variables
# that Jenkins sets. This allows Jenkins to locate the test after it has run:

# caps['name'] = "Hard-coded build name"
# caps['build'] = "1.0.0"
# caps['jenkins_name'] = os.environ.get("CBT_BUILD_NAME")
# caps['jenkins_build'] = os.environ.get("CBT_BUILD_NUMBER")

class SeleniumCBT(unittest.TestCase):
    def setUp(self):

        # start the remote browser on our server
        self.driver = webdriver.Remote(
            desired_capabilities=caps,
            command_executor="http://%s:%s@hub.crossbrowsertesting.com:80/wd/hub" % (username, key)
        )

        self.driver.implicitly_wait(20)

    def test_CBT(self):

        # load the page url
        print('Loading Url')
        self.driver.get('http://crossbrowsertesting.github.io/selenium_example_page.html')

        # maximize the window - DESKTOPS ONLY
        # print('Maximizing window')
        # self.driver.maximize_window()

        # check the title
        print('Checking title')
        self.assertTrue("Selenium Test Example Page" in self.driver.title)

    def tearDown(self):
        print("Done with session %s" % self.driver.session_id)
        self.driver.quit()

if __name__ == '__main__':
    unittest.main()
