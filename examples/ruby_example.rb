#!/usr/local/bin/ruby
# Please visit here https://code.google.com/p/selenium/wiki/RubyBindings for detailed installation and instructions

require "selenium-webdriver"

caps = Selenium::WebDriver::Remote::Capabilities.new

username = ENV["CBT_USERNAME"]
key = ENV["CBT_APIKEY"]
caps['name'] = ENV["CBT_BUILD_NAME"]    # Need to set these to something else?
caps['build'] = ENV["CBT_BUILD_NUMBER"] # Check below to learn how
caps['browser_api_name'] = ENV["CBT_BROWSER"]
caps['os_api_name'] = ENV["CBT_OPERATING_SYSTEM"]
caps['screen_resolution'] = ENV["CBT_RESOLUTION"]
caps["record_video"] = "true"

# Need to set the build and name capabilites to something else?
# Just set the jenkins_name and jenkins_build caps to the proper environment variables
# that Jenkins sets. This allows Jenkins to locate the test after it has run:

# caps['name'] = "Hard-coded build name"
# caps['build'] = "1.0.0"
# caps['jenkins_name'] = ENV["CBT_BUILD_NAME"]
# caps['jenkins_build'] = ENV["CBT_BUILD_NUMBER"]

driver = Selenium::WebDriver.for(:remote,
:url => "http://#{username}:#{key}@hub.crossbrowsertesting.com:80/wd/hub",
:desired_capabilities => caps)

#maximize the window - DESKTOPS ONLY
#driver.navigate.to "http://crossbrowsertesting.github.io/selenium_example_page.html"
#driver.manage.window.maximize

puts driver.title

driver.quit