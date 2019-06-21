#!/usr/bin/env groovy

import groovy.json.JsonSlurperClassic
import hudson.tasks.test.AbstractTestResultAction


def call(def args) {
  def msg = args.msg ?: ""
  def color = args.color ?: ""
  def channel = args.channel ?: ""

  // fetch arguments
  def buildMsg
  def testResultMsg
  if (args.currentBuild) {
    buildMsg = getBuildMsg(args.currentBuild)
    testResultMsg = getTestResult(args.currentBuild)
  }
  if (args.buildGit) {
    buildMsg["git"] = parseJson(args.buildGit)
  }
  if (args.testResultGit) {
    testResultMsg["git"] = parseJson(args.testResultGit)
  }
  def verifierResultMsg = []
  if (args.verifierResult) {
    print "args.verifierResult: ${args.verifierResult}"
    for (entry in args.verifierResult) {
      print "entry: ${entry.key}: ${entry.value}"
      def buf = getVerifierResultMsg(entry.key, entry.value)
      if (buf) {
        verifierResultMsg.add(buf)
      }
    }
  }

  // buildMsg
  if (buildMsg) {
    color = buildMsg.color
    msg += msg ? "\n    " : ""
    msg += buildMsg.msg
    msg += buildMsg.git ? " (${buildMsg.git.msg})" : ""
  }

  // testResultMsg
  if (testResultMsg) {
    msg += msg ? "\n    " : ""
    msg += testResultMsg.msg
    msg += testResultMsg.git ? " (${testResultMsg.git.msg})" : ""
  }

  // verifierResultMsg
  if (verifierResultMsg) {
    for (entry in verifierResultMsg) {
      msg += msg ? "\n    " : ""
      msg += entry.msg
    }
  }

  // slackSend
  slackSend(message: msg, color: color, channel: channel)
}


def parseJson(txt) {
  return new JsonSlurperClassic().parseText(txt)
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

def getVerifierResultMsg(def targetName, def res) {
  if (!res.verify) {
    return [:]
  }
  def summary = res.verifyErr ?: res.verify
  def msg = "${targetName} (${res.verifyCurNo}/${res.verifyBestNo}): ${summary}"
  return [msg: msg, target: targetName, verify: res.verify, bestBlockNo: res.verifyBestNo, currBlockNo: res.verifyCurNo, err: res.verifyErr]
}
