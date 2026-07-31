#!/usr/bin/env python3
"""SSH/SFTP helper for deployment."""
import os
import sys
import time
import paramiko

HOST = os.environ.get("DEPLOY_SSH_HOST", "your.server.ip")
USER = os.environ.get("DEPLOY_SSH_USER", "root")
PASSWORD = os.environ.get("DEPLOY_SSH_PASSWORD", "")
PORT = int(os.environ.get("DEPLOY_SSH_PORT", "22"))


def connect():
    if not PASSWORD:
        raise RuntimeError("Set DEPLOY_SSH_PASSWORD (and optionally DEPLOY_SSH_HOST/USER/PORT)")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, port=PORT, username=USER, password=PASSWORD, timeout=30)
    return client


def run(cmd, timeout=600):
    client = connect()
    try:
        stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout, get_pty=True)
        out_chunks = []
        while True:
            if stdout.channel.recv_ready():
                chunk = stdout.channel.recv(4096).decode("utf-8", errors="replace")
                out_chunks.append(chunk)
                sys.stdout.write(chunk)
                sys.stdout.flush()
            if stdout.channel.exit_status_ready() and not stdout.channel.recv_ready():
                break
            time.sleep(0.05)
        # drain remaining
        rest = stdout.read().decode("utf-8", errors="replace")
        if rest:
            out_chunks.append(rest)
            sys.stdout.write(rest)
        err = stderr.read().decode("utf-8", errors="replace")
        if err:
            sys.stderr.write(err)
        code = stdout.channel.recv_exit_status()
        return code
    finally:
        client.close()


def upload(local_path, remote_path):
    client = connect()
    try:
        sftp = client.open_sftp()
        # ensure remote dir
        remote_dir = os.path.dirname(remote_path).replace("\\", "/")
        parts = remote_dir.strip("/").split("/")
        cur = ""
        for p in parts:
            cur += "/" + p
            try:
                sftp.stat(cur)
            except IOError:
                sftp.mkdir(cur)
        print(f"Uploading {local_path} -> {remote_path} ...")
        sftp.put(local_path, remote_path)
        print("Upload done.")
    finally:
        client.close()


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: _deploy_ssh.py run <cmd> | upload <local> <remote>")
        raise SystemExit(2)
    action = sys.argv[1]
    if action == "run":
        raise SystemExit(run(" ".join(sys.argv[2:])))
    if action == "upload":
        upload(sys.argv[2], sys.argv[3])
        raise SystemExit(0)
    raise SystemExit(run(" ".join(sys.argv[1:])))
