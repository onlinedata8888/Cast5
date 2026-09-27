# Cast integration

The second-page **Cast** button now uses the same functional flow as the old Cast Remote project, adapted to this WebView/Android TV Remote v2 project.

## Flow

1. The currently connected TV IP is reused as the Cast target.
2. The Cast button opens Photos / Videos / Audio selection through Android Storage Access Framework.
3. The selected `content://` URI is exposed by the phone through a small local HTTP server with byte-range support.
4. `CastClient` connects to the TV's Google Cast v2 endpoint on TCP 8009 using TLS.
5. It launches the Default Media Receiver (`CC1AD845`) and sends a Cast `LOAD` message.
6. Playback status is returned to the WebView.
7. Play/Pause, Seek and Stop are wired to the Cast media namespace.

The old project's Display tab was only an informational placeholder (“Pick Photos, Videos, or Audio to start casting”), so this integration does not claim to implement screen mirroring.

## Source files

- `app/java/com/example/tvremote/CastClient.java`
- `app/java/com/example/tvremote/CastBridge.java`
- `app/java/com/example/tvremote/CastPickerActivity.java`
- `app/java/com/example/tvremote/MediaHttpServer.java`
- `app/java/com/example/tvremote/TvBridge.java`
- `app/assets/remote.html`

`build_apk.sh` already compiles every Java file in `app/java/com/example/tvremote/*.java`, so these classes are included when the normal build script is run.

Protocol background: Google Cast v2 uses TLS on port 8009 and length-prefixed protobuf `CastMessage` packets; the sender then uses the receiver/media namespaces for launch and media control.
