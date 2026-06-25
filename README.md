# Worship Team Organizer

Worship Team Organizer is a Java client-server application for planning worship team events. It helps users manage event schedules, service topics, song lists, team members, and user activity records. This is currently a local-only multi-client project: the server and clients are intended to run on the same machine using `localhost`.

## Requirements

- Java JDK installed
- macOS/Linux terminal or another shell that can run `.sh` scripts
- Port `1234` available for the server

## Quick Start

Open one terminal for the server:

```bash
cd /Users/annawang-11/Documents/GitHub/Worship-Team-Organizer
./run_server.sh
```

When the server starts correctly, it prints:

```text
Multi-client server started on port 1234. Waiting for clients...
```

Open a second terminal for the first client:

```bash
cd /Users/annawang-11/Documents/GitHub/Worship-Team-Organizer
./run_client.sh
```

Open more terminals and run the same client command if you want multiple users online at the same time.

For the multi-user GUI demo, start separate client windows with:

```bash
./run_client2.sh
./run_client3.sh
```

Run each command in a different terminal while the server is still running.

The server keeps running even after every client window is closed. It waits for the next client until you manually stop it with `Ctrl+C` in the server terminal.

## Login and Register

When the client window opens:

1. Enter a username and password.
2. Click `Log in` if the user already exists.
3. Click `Create new user` to register a new account.

Example existing test user:

```text
Username: Anna
Password: a
```

## Using the Dashboard

After login, the main dashboard shows:

- Calendar: choose a date and see how many events are scheduled.
- Event / Topic / Song: add events, edit topics, edit songs, and delete events.
- Who Plays What: add or remove team members for the selected event.
- User Activity: view recent changes made by users.

To add an event:

1. Click a date on the calendar.
2. Click `Add Event`.
3. Enter the time in `HH:mm` format, for example `16:08`.
4. Enter the event title.
5. Confirm the dialog.

To edit songs or topics:

1. Select an event from the event list.
2. Use `Edit Topic` or `Edit Songs`.

To assign team members:

1. Select an event.
2. Use `Add Member` in the `Who Plays What` panel.
3. Select a member and click `Delete` to remove that member from the event.

## Multi-Client Collaboration

This project supports multiple GUI clients connected to one server at the same time.

To test collaboration:

1. Start the server with `./run_server.sh`.
2. Start one client with `./run_client.sh` and log in.
3. Start another client with `./run_client2.sh` or `./run_client3.sh` and register or log in as another user.
4. Add or edit an event in one client.
5. The other connected client refreshes its dashboard data automatically.

The server logs each connected client and command. Useful log examples:

```text
[SERVER] Accepted client ...
[SERVER] Online clients: 2
Socket[...]:REGISTER|username|password
[SERVER] Reply to ...: REGISTER_SUCCESS
```

## Troubleshooting

If a client times out:

1. Stop all running server and client windows.
2. Start the server again with `./run_server.sh`.
3. Confirm the server prints `Multi-client server started on port 1234. Waiting for clients...`.
4. Start clients with `./run_client.sh`.

If the server says the port is already in use, another server is still running. Stop the old server first, then run `./run_server.sh` again.

Do not start the server from a random directory with a manual `java Server` command. The app stores data in CSV files inside `WT_Server`, and `run_server.sh` starts from the correct folder.

If all clients are closed, the server should print:

```text
[SERVER] No clients connected. Server is still waiting for new clients.
```

This is normal. Leave the server running if you want to open another client later.

## Project Structure

```text
WT_Server/
  src/
    Server.java
    model/
    storage/
  events.csv
  users.csv
  userActivitiesLog.csv

WT_Client/
  src/
    Client.java
    ClientConnection.java
    Login.java
    MainFrame.java

run_server.sh
run_client.sh
```

## Data Files

The server stores app data in CSV files:

- `WT_Server/users.csv`: registered users and team member data
- `WT_Server/events.csv`: event, topic, song, and team assignment data
- `WT_Server/userActivitiesLog.csv`: user activity history

## Acknowledgement

The GUI and multi-user features of this project were completed with assistance from Codex.
