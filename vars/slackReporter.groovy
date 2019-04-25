#!/usr/bin/env groovy

import hudson.tasks.test.AbstractTestResultAction

@NonCPS
def call(def currentBuild) {
  def buildResult = currentBuild.result
  if (!buildResult) {
    buildResult = currentBuild.currentResult
  }
  def COLOR_MAP = ['SUCCESS': 'good', 'UNSTABLE': 'warning', 'FAILURE': 'danger', 'ABORTED': 'danger']
  def color = COLOR_MAP[buildResult]
  def msg = "<${env.JOB_URL}|${env.JOB_NAME}> - Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}>: ${buildResult}"
  AbstractTestResultAction testResults =  currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
  if (testResults != null) {
    def total = testResults.totalCount
    def failed = testResults.failCount
    def skipped = testResults.skipCount
    def passed = total - failed - skipped
    msg += "\n  Test Status: Total ${total}, Passed ${passed}, Failed ${failed}, Skipped ${skipped}"
  }
  return slackSend(message: msg, color: color)
}
