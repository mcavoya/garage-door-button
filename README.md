# garage-door-button
TLDR; SmartThings device handler and SmartApp to convert a Z-Wave relay into a garage door opener button.

The motivation for this project was the lack of (garage) door support by Google's Assistant.

I very much want to say "hey Google, open the garage door" while I'm still half a block from home, and have the door open and waiting for me when I get there. I also want the opener to be smart enough not to just "push the button" and actually close the door if it was already open. Finally, I want this to be completely hands-off because I normally commute via motorcycle; meaning my phone is in my pocket, and I am talking to it though the Bluetooth connected headset in my helmet.

I know there are some other solutions out there. Some work with MyQ, some work with Linear's GD00Z-4. But, I had a simple Linear FS20Z-1 relay just laying around looking for a problem to solve. So I wrote a two part solution that can work with any Z-Wave relay and any door opener.

The first part of the solution is a custom Z-Wave Momentary Relay device handler. I used the SmartSense Virtual Momentary Contact Switch by SmartThings as a starting point.

I extended this device handler to modify its behavior depending on the open/closed state of a door. The handler keeps track of the current garage door state (as provided by the sister app); and, if the relay is told to "turn off" (close the door) when garage door is already closed, then it will not cycle the relay. Same idea when the door is open, a command to "turn on" the relay (open the door) will not cycle the relay.

I assigned this device handler to my Linear FS20Z-1 relay, which is connected in parallel to my garage door opener's push button. I then renamed the relay to "Garage Door Button." Note that at this point, the push button in the SmartThings app would not activate the door. This is because the garage door's state is unknown by the handler until after the next step.

The sister app for the Z-Wave momentary relay device handler is what turns the Z-Wave Momentary Relay into a garage door opener "button." After installing the app, select the door and relay you would like to link together. That's it. There's nothing else to configure in this app. The app simply subscribes to the door's events, and when it sees the door opening or closing (by any means), it updates the relay handler with the current state. Open or close the door manually once to synchronize the handler, and you can then open/close the door by pressing the "Garage Door Button" in SmartThings.

Obviously, at this point, we haven't really gained anything. If you have a SmartThings garage door opener, then there's probably already a SmartThings button that can be pushed to open/close the door. The real magic is in what happens next.

The final step is to add the "Garage Door Button" to the Google Home app. That's it. To the Assistant, the relay looks like an ordinary on/off switch. Even though it has "door" in the name, the Assistant does not consider this a security issue and allows me to "turn on" the switch by saying "open the garage door." I can "turn off" the switch" by saying "close the garage door." Note that even though the Assistant believes it is turning the switch on and off, the relay is really just momentarily closing its contacts.

I added two bits of precaution to the device handler software. The Z-Wave command to close the contacts is followed by three commands to open the contacts with some delay between every command. This is to reduce the likelihood of the relay being left in the closed state (and preventing the wall button from working). The second bit of precaution is an extra Z-Wave command in the refresh function to open the contacts, and then return the state of the relay. This way, if the relay is left in the closed state, hitting refresh will hopefully force them open.

So that's it. With a couple pieces of code and an old relay, I finally have the smart voice control that I've always wanted over my garage door. Hopefully, by posting this on GitHub, I can help someone else with an old relay.

- Audi
