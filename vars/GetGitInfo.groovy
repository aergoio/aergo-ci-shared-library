#!/usr/bin/env groovy

import groovy.json.JsonOutput

def call() {
	def ref = sh(script: 'git symbolic-ref --short -q HEAD || git rev-parse --short HEAD', returnStdout: true).trim()
	def rev = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
	def url = sh(script: 'git remote get-url origin', returnStdout: true).trim()
	def msg = "${rev}"
	def link = url
	if (link) {
		link = link.replace(".git", "")
		if (link.startsWith("git@")) {
			link = link.replace(":", "/").replace("git@", "https://")
		}
		link += "/commit/" + rev
		msg = "<${link}|${rev}>"
	}
  if (ref != rev) {
	  msg += " ${ref}"
	}
	msg = "(" + msg + ")"
	return JsonOutput.toJson([msg: msg, rev: rev, ref: ref, url: url, link: link])
}
