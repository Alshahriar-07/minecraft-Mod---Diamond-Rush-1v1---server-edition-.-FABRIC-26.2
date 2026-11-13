#!/usr/bin/env python3
"""Tiny RCON client used to drive the Diamond Rush test server."""
import socket
import struct
import sys

HOST = "127.0.0.1"
PORT = 25575
PASSWORD = "drtest"


class Rcon:
    def __init__(self):
        self.sock = socket.create_connection((HOST, PORT), timeout=10)
        self.request_id = 0
        self._send(3, PASSWORD)
        self._recv()

    def _send(self, packet_type, body):
        self.request_id += 1
        payload = struct.pack("<ii", self.request_id, packet_type) + body.encode("utf-8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(payload)) + payload)
        return self.request_id

    def _recv(self):
        raw_length = self._read_exact(4)
        length = struct.unpack("<i", raw_length)[0]
        data = self._read_exact(length)
        return data[8:].rstrip(b"\x00").decode("utf-8", errors="replace")

    def _read_exact(self, count):
        chunks = b""
        while len(chunks) < count:
            chunk = self.sock.recv(count - len(chunks))
            if not chunk:
                raise ConnectionError("RCON connection closed")
            chunks += chunk
        return chunks

    def command(self, command):
        self._send(2, command)
        return self._recv()

    def close(self):
        self.sock.close()


if __name__ == "__main__":
    client = Rcon()
    try:
        for command in sys.argv[1:]:
            print(f"> {command}")
            print(client.command(command))
    finally:
        client.close()
