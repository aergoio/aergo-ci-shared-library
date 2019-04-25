#!/usr/bin/env groovy

import groovy.json.JsonSlurper
import hudson.tasks.test.AbstractTestResultAction

def call(def args) {
  def msg = args.msg ?: ""
  def color = args.color ?: ""
  def channel = args.channel ?: ""

  // buildMsg
  if (args.currentBuild) {
    def buildMsg = getBuildMsg(args.currentBuild)
    if (buildMsg) {
      msg = buildMsg.msg
      color = buildMsg.color
    }
  }

  // gitInfo
  if (args.gitInfo) {
    def gitInfo = parseJson(args.gitInfo)
    if (gitInfo) {
      msg += " ${gitInfo.msg}"
    }
  }

  // testResult
  if (args.currentBuild) {
    def testResult = getTestResult(args.currentBuild)
    if (testResult) {
      msg += "\n  ${testResult.msg}"
      // testsetInfo
      if (args.testsetInfo) {
        def testsetInfo = parseJson(args.testsetInfo)
        if (testsetInfo) {
          msg += " ${testsetInfo.msg}"
        }
      }
    }
  }

  // slackSend
  return slackSend(message: msg, color: color, channel: channel)
}


def parseJson(txt) {
  return new JsonSlurper().parseText(txt)
}


def getBuildMsg(def currentBuild) {
  def status = currentBuild.result
  if (!status) {
    status = currentBuild.currentResult
  }
  def color = null
  switch (status) {
	  case 'SUCCESS':
	  	color = 'good'
  	  break
	  case 'UNSTABLE':
	  	color = 'warning'
  	  break
	  case 'FAILURE':
	  case 'ABORTED':
	  default:
	  	color = 'danger'
      break
  }
  def msg = "<${env.JOB_URL}|${env.JOB_NAME}> - Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}>: ${status}"
  return [status: status, color: color, msg: msg]
}


@NonCPS
def getTestResult(def currentBuild) {
  if (!currentBuild.rawBuild) {
    return null
  }
  AbstractTestResultAction tr =  currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
  if (tr == null) {
    return null
  }
  def total = tr.totalCount
  def failed = tr.failCount
  def skipped = tr.skipCount
  def passed = total - failed - skipped
  def msg = "Test Status: Total ${total}, Passed ${passed}, Failed ${failed}, Skipped ${skipped}"
  return [msg: msg, total: total, failed: failed, skipped: skipped, passed: passed]
}
