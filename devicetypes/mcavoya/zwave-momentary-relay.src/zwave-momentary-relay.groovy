/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Z-Wave Momentary Relay
 *  Based on SmartSense Virtual Momentary Contact Switch by SmartThings
 *
 *
 *  This device handler has been extended to modify its behavior depending on the open/closed state of a door.
 *  I assigned this device handler to a Linear FS20Z-1 relay, which is connected in parallel to my garage door opener's push button.
 *	So in my use case, this device keeps track of the current garage door state (as provided by the sister app); and,
 *  if the relay is told to "turn off" (close the door) when garage door is already closed, then it will not cycle the relay.
 *  Same idea when the door is open, a command to "turn on" the relay (open the door) will not cycle the relay.
 *
 *  Author: Audi McAvoy
 *  Date: 01 October 2018
 */
metadata {
	definition (name: "Z-Wave Momentary Relay", namespace: "mcavoya", author: "Audi McAvoy", ocfDeviceType: "x.com.st.d.momentary.realy") {
		capability "Actuator"
		capability "Switch"
		capability "Momentary"
		capability "Refresh"
		capability "Sensor"
        // define custom attribute and commands to support garage door function
        attribute 'door', 'enum', ['open', 'closed']
        command 'doorOpen'
        command 'doorClosed'
	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		// reply messages
		reply "2001FF,2502,delay 2000,200100,2502": "command: 2503, payload: FF"
		reply "200100,2502": "command: 2503, payload: 00"
	}

	// tile definitions
	tiles {
		standardTile('switch', 'device.switch', width: 2, height: 2, canChangeIcon: true) {
			state 'off', label: 'push', action: 'momentary.push', icon: 'st.switches.switch.off', backgroundColor: '#ffffff'
			state 'on', label: 'pressed', action: 'switch.off', icon: 'st.switches.switch.on', backgroundColor: '#00a0dc'
		}
		standardTile('refresh', 'device.switch', inactiveLabel: false, decoration: 'flat') {
			state 'default', label:'', action:'refresh.refresh', icon:'st.secondary.refresh'
		}

		main 'switch'
		details(['switch', 'refresh'])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x20: 1])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	log.debug "Parse returned ${result?.descriptionText}"
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "physical"]
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
	[name: "switch", value: cmd.value ? "on" : "off", type: "digital"]
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	if (state.manufacturer != cmd.manufacturerName) {
		updateDataValue("manufacturer", cmd.manufacturerName)
	}

	final relays = [
	    [manufacturerId:0x0113, productTypeId: 0x5246, productId: 0x3133, productName: "Evolve LFM-20"],
		[manufacturerId:0x5254, productTypeId: 0x8000, productId: 0x0002, productName: "Remotec ZFM-80"]
	]

	def productName  = null
	for (it in relays) {
		if (it.manufacturerId == cmd.manufacturerId && it.productTypeId == cmd.productTypeId && it.productId == cmd.productId) {
			productName = it.productName
			break
		}
	}

	if (productName) {
		log.debug "Relay found: $productName"
		updateDataValue("productName", productName)
	}
	[name: "manufacturer", value: cmd.manufacturerName]
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

// actions to keep track of door state

def doorOpen() {
	state.door = "open"
    log.debug "New garage door state: $state.door"
}

def doorClosed() {
	state.door = "closed"
    log.debug "New garage door state: $state.door"
}

// actions to run the momentary push button

def push() {
	if ("busy" == state.door) {
    	log.debug "Patience, don't push the button so fast"
    }
    // My Home has several modes defined, home, away, sleep, a few others. In "Sleep" mode, none of the doors
    // can be unlocked or opened by SmartThings automations. If you implement a similar strategy, then you may
    // want to change the following mode to match your scheme. If you leave the statement as is, and your house
    // never goes into sleep mode, then this statement will have no affect.
	else if ("Sleep" == location.mode) {
    	log.debug "Home in Sleep mode, did NOT push button"
    }
    else {
  		log.debug "I pushed the button"
        state.door = "busy"
        [
            zwave.basicV1.basicSet(value: 0xFF).format(),
            zwave.switchBinaryV1.switchBinaryGet().format(),
            "delay 250",
            zwave.basicV1.basicSet(value: 0x00).format(),
            "delay 500",
            zwave.basicV1.basicSet(value: 0x00).format(),
            "delay 1000",
            zwave.basicV1.basicSet(value: 0x00).format(),
            zwave.switchBinaryV1.switchBinaryGet().format()
        ]
	}
}

def on() { // open the door
    log.debug "Attempt to open door while door is $state.door"
	if (state.door == "closed") {
    	log.debug "Pushing the button to open the door"
		push()
    }
}

def off() { // close the door
    log.debug "Attempt to close door while door is $state.door"
	if (state.door == "open") {
    	log.debug "Pushing the button to close the door"
		push()
    }
}

def refresh() { // also releases the relay in case it is stuck in the pressed position
	delayBetween([
        zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchBinaryV1.switchBinaryGet().format(),
		zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	])
}
