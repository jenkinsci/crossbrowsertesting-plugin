from threading import Thread
from selenium import webdriver
import time, os, requests

USERNAME = os.environ.get("CBT_USERNAME")
API_KEY = os.environ.get("CBT_AUTHKEY")
api_session = requests.Session()
api_session.auth = (USERNAME,API_KEY)

def get_browser(caps):
    return webdriver.Remote(
            desired_capabilities=caps,
            command_executor="http://%s:%s@hub.crossbrowsertesting.com:80/wd/hub" % (USERNAME, API_KEY)
        )
list = os.environ.get("CBT_BROWSERS")
browsers = eval(list)
for i in browsers:
    i['os_api_name']= i.pop('operating_system')
    i['browser_api_name']= i.pop('browser')

browsers_waiting = []


def get_browser_and_wait(browser_data):
    print ("starting %s\n" % browser_data["browserName"])
    browser = get_browser(browser_data)
    browser.get("http://crossbrowsertesting.com")
    browsers_waiting.append({"data": browser_data, "driver": browser})
    print ("%s ready" % browser_data["browserName"])
    while len(browsers_waiting) < len(browsers):
        print ("working on %s.... please wait" % browser_data["browserName"])
        browser.get("http://crossbrowsertesting.com")
        time.sleep(3)

def set_score(score, session_id):
    if score is not None:
        api_session.put('https://crossbrowsertesting.com/api/v3/selenium/' + session_id,
            data={'action':'set_score', 'score':score})

threads = []
for i, browser in enumerate(browsers):
    thread = Thread(target=get_browser_and_wait, args=[browser])
    threads.append(thread)
    thread.start()


for thread in threads:
    thread.join()

print ("all browsers ready")
for i, b in enumerate(browsers_waiting):
    try:    
        print ("title: %s" % (b["driver"].title))
        b["driver"].quit()
        set_score('pass',b["driver"].session_id) 
    
    except AssertionError as e:
        score = 'fail'
        driver.quit()
        set_score('fail',b["driver"].session_id) 


