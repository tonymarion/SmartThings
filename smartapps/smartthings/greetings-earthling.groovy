/**
 *  Greetings Earthling
 *
 *  Author: SmartThings
 *  Date: 2013-03-07
 */
preferences {

	section("When one of these people arrive at home") {
		input "people", "capability.presenceSensor", multiple: true
	}
	section("Change to this mode") {
		input "newMode", "mode", title: "Mode?"
	}
	section("False alarm threshold (defaults to 10 min)") {
		input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
		input "phone", "phone", title: "Send a Text Message?", required: false
	}

}

def installed() {
	log.debug "Installed with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	subscribe(people, "presence", presence)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	log.debug "Current mode = ${location.mode}, people = ${people.collect{it.label + ': ' + it.currentPresence}}"
	unsubscribe()
	subscribe(people, "presence", presence)
}

def presence(evt)
{
	log.debug "evt.name: $evt.value"
	def threshold = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? (falseAlarmThreshold * 60 * 1000) as Long : 10 * 60 * 1000L

	if (location.mode != newMode) {

		def t0 = new Date(now() - threshold)
		if (evt.value == "present") {

			def person = getPerson(evt)
			def recentNotPresent = person.statesSince("presence", t0).find{it.value == "not present"}
			if (recentNotPresent) {
				log.debug "skipping notification of arrival of ${person.displayName} because last departure was only ${now() - recentNotPresent.date.time} msec ago"
			}
			else {
				def message = "${person.displayName} arrived at home, changing mode to '${newMode}'"
				log.info message
				send(message)
				setLocationMode(newMode)
			}
		}
	}
	else {
		log.debug "mode is the same, not evaluating"
	}
}

private getPerson(evt)
{
	people.find{evt.deviceId == it.id}
}

private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phone ) {
		log.debug( "sending text message" )
		sendSms( phone, msg )
	}

	log.debug msg
}