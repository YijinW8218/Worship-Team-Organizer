# Worship-Team-Organizer
- This is a class project for CMPSC211.
- This project aims to create a system to help church worship team organize their events schedules, songs selections, and members' working schedule.

## Features

- Java swing
- Client-server architecture
- Multi-client GUI support: multiple users can connect to the same server at the same time.
- Real-time refresh: when one user changes events, songs, members, or topics, other connected GUI clients refresh their dashboard data.

## Run the app

Start the server first:

```bash
./run_server.sh
```

Then open one or more clients in separate terminals:

```bash
./run_client.sh
```

The scripts compile the latest Java source before running. The server script also starts from the `WT_Server` folder so the app uses the correct CSV data files.

## Notes

- The server listens on port `1234`.
- Keep the server running while clients are open.
- If a client times out, restart the server with `./run_server.sh` and check that the server prints `Multi-client server started on port 1234. Waiting for clients...`.
