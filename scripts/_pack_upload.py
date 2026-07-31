#!/usr/bin/env python3
"""Pack project artifacts and upload to server."""
import os
import tarfile
import tempfile
import paramiko

HOST = os.environ.get("DEPLOY_SSH_HOST", "your.server.ip")
USER = os.environ.get("DEPLOY_SSH_USER", "root")
PASSWORD = os.environ.get("DEPLOY_SSH_PASSWORD", "")
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
REMOTE_DIR = os.environ.get("DEPLOY_REMOTE_DIR", "/www/wwwroot/your-site")
REMOTE_TAR = "/tmp/sjk-deploy.tar.gz"


def should_exclude(name: str) -> bool:
    parts = name.replace("\\", "/").split("/")
    skip = {
        "node_modules",
        ".git",
        "target",
        ".idea",
        ".vscode",
        "coverage",
        "dist",
        "__pycache__",
        ".husky",
    }
    return any(p in skip for p in parts)


def main():
    fd, local_tar = tempfile.mkstemp(suffix=".tar.gz")
    os.close(fd)
    print(f"Packing -> {local_tar}")
    with tarfile.open(local_tar, "w:gz") as tar:
        # backend source
        backend = os.path.join(ROOT, "backend")
        for dirpath, dirnames, filenames in os.walk(backend):
            dirnames[:] = [d for d in dirnames if d not in {"target", ".idea"}]
            for fn in filenames:
                full = os.path.join(dirpath, fn)
                arc = os.path.relpath(full, ROOT).replace("\\", "/")
                tar.add(full, arcname=arc)
        # frontend dist
        dist = os.path.join(ROOT, "frontend", "dist")
        for dirpath, dirnames, filenames in os.walk(dist):
            for fn in filenames:
                full = os.path.join(dirpath, fn)
                # place into deploy/frontend
                rel = os.path.relpath(full, dist).replace("\\", "/")
                tar.add(full, arcname=f"deploy-frontend/{rel}")
        # docker helper files
        for rel in [
            "docker/docker-compose.yml",
            "docker/Dockerfile.backend",
            "docker/Dockerfile.frontend",
            "docker/.env",
            "docker/nginx-host.example.conf",
            "nginx.conf",
        ]:
            full = os.path.join(ROOT, rel)
            if os.path.exists(full):
                tar.add(full, arcname=rel)

    size_mb = os.path.getsize(local_tar) / 1024 / 1024
    print(f"Archive size: {size_mb:.1f} MB")

    if not PASSWORD:
        raise RuntimeError("Set DEPLOY_SSH_PASSWORD (and optionally DEPLOY_SSH_HOST/USER)")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, username=USER, password=PASSWORD, timeout=30)
    sftp = client.open_sftp()
    print(f"Uploading to {REMOTE_TAR} ...")
    sftp.put(local_tar, REMOTE_TAR)
    sftp.close()
    print("Upload complete.")
    client.close()
    os.remove(local_tar)


if __name__ == "__main__":
    main()
