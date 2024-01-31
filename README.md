# VIP Belote inspector

Modules:

* `vipbelote-chrome-inspector`: executable that connects to a Chrome browser (with debugger enabled) and eavesdrop the
  web socket traffic of the game to print the game states.
 
* `vipbelote-protocol`: defines a typed model for all protocol messages in the game, and the `VipBeloteDecoder` class
  to decode web socket text frames into `VipBelotePacket`s containing these messages.
 
* `vipbelote-records-analyzer`: executable that:
  1. filters HAR files from `data/har-raw` (if any) by removing all non-websocket traffic
  2. places the filtered files into `data/har-filtered`
  3. decodes websocket messages from the filtered HAR files and writes the messages by namespace in `data/decoded` 

* `vipbelote-state`: defines a model of the state of the game, and a reducer to update the state based on messages