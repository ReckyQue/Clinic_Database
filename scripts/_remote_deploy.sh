#!/bin/bash
set -euo pipefail

SITE=/www/wwwroot/<your_domain>
JAVA17=/www/server/java/jdk-17.0.8/bin/java
JAVA_HOME17=/www/server/java/jdk-17.0.8
MAVEN_HOME=/opt/maven
PATH_EXTRA="$MAVEN_HOME/bin:$JAVA_HOME17/bin"
export JAVA_HOME="$JAVA_HOME17"
export PATH="$PATH_EXTRA:$PATH"

echo "[1/8] Prepare dirs & extract"
mkdir -p "$SITE/build" "$SITE/frontend" "$SITE/backup"
TS=$(date +%Y%m%d_%H%M%S)
if [ -f "$SITE/backend.jar" ]; then
  cp -f "$SITE/backend.jar" "$SITE/backup/backend.jar.$TS" || true
fi
rm -rf "$SITE/build"/*
mkdir -p "$SITE/build"
tar -xzf /tmp/sjk-deploy.tar.gz -C "$SITE/build"

echo "[2/8] Install frontend dist"
rm -rf "$SITE/frontend"/*
cp -a "$SITE/build/deploy-frontend/." "$SITE/frontend/"

echo "[3/8] Ensure Maven"
if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  echo "Installing Maven from /tmp/apache-maven-3.9.16-bin.tar.gz ..."
  tar -xzf /tmp/apache-maven-3.9.16-bin.tar.gz -C /opt
  ln -sfn /opt/apache-maven-3.9.16 /opt/maven
fi
mvn -version

echo "[4/8] Free memory for build (stop old backend; pause ES if present)"
pkill -f '/www/wwwroot/<your_domain>/backend.jar' || true
pkill -f 'java -jar -Dspring.profiles.active=prod backend.jar' || true
sleep 2
# Elasticsearch occupies ~500MB; stop temporarily for compile
if pgrep -f 'elasticsearch' >/dev/null 2>&1; then
  echo "Stopping elasticsearch temporarily..."
  pkill -f 'org.elasticsearch.bootstrap.Elasticsearch' || true
  sleep 3
  RESTART_ES=1
else
  RESTART_ES=0
fi
sync; echo 3 > /proc/sys/vm/drop_caches 2>/dev/null || true
free -h

echo "[5/8] Build backend jar"
cd "$SITE/build/backend"
mkdir -p /root/.m2
cat > /root/.m2/settings.xml <<'XML'
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>*</mirrorOf>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
XML
mvn -q -DskipTests clean package
JAR=$(ls -1 target/*.jar | grep -v original | head -1)
cp -f "$JAR" "$SITE/backend.jar"
ls -lh "$SITE/backend.jar"

echo "[6/8] Write nginx (80 + 9090 -> static; /api -> 9091)"
cat > /www/server/panel/vhost/nginx/<your_domain>.conf <<'NGINX'
server {
    listen 80;
    listen 9090;
    server_name <your_domain>;

    client_max_body_size 50m;

    location / {
        root /www/wwwroot/<your_domain>/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;

        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }

    location /api/ {
        proxy_pass http://127.0.0.1:9091/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 30s;
        proxy_send_timeout 120s;
        proxy_read_timeout 120s;
        proxy_http_version 1.1;
    }

    access_log /www/wwwlogs/<your_domain>.access.log;
    error_log /www/wwwlogs/<your_domain>.error.log;
}
NGINX
nginx -t
nginx -s reload

echo "[7/8] Start backend on 9091 with host MySQL + Redis"
cat > "$SITE/start.sh" <<'EOF'
#!/bin/bash
SITE=/www/wwwroot/<your_domain>
JAVA17=/www/server/java/jdk-17.0.8/bin/java
cd "$SITE"
pkill -f "$SITE/backend.jar" || true
sleep 2
nohup env \
  SPRING_PROFILES_ACTIVE=prod \
  SERVER_PORT=9091 \
  DB_HOST=127.0.0.1 \
  DB_PORT=3306 \
  DB_NAME=<your_db_name> \
  DB_USER=<your_db_name> \
  DB_PASSWORD=<your_db_password> \
  DB_SSL=false \
  REDIS_HOST=127.0.0.1 \
  REDIS_PORT=6379 \
  JAVA_TOOL_OPTIONS="-Xmx384m -Xms192m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:MaxMetaspaceSize=128m" \
  "$JAVA17" -jar "$SITE/backend.jar" > "$SITE/app.log" 2>&1 &
echo $! > "$SITE/backend.pid"
echo "started pid=$(cat $SITE/backend.pid)"
EOF
chmod +x "$SITE/start.sh"
bash "$SITE/start.sh"

echo "[8/8] Wait health / canary"
ok=0
for i in $(seq 1 60); do
  code=$(curl -sS -m 3 -o /tmp/canary.json -w '%{http_code}' http://127.0.0.1:9091/api/v1/canary/probe || true)
  if [ "$code" = "200" ]; then
    echo "canary OK: $(cat /tmp/canary.json)"
    ok=1
    break
  fi
  echo "waiting backend... ($i) http=$code"
  sleep 3
done

if [ "$RESTART_ES" = "1" ]; then
  echo "Note: Elasticsearch was stopped for build; restart manually if needed."
fi

echo '=== ports ==='
ss -lntp | grep -E '9090|9091|80 ' || true
echo '=== tail log ==='
tail -40 "$SITE/app.log" || true

if [ "$ok" != "1" ]; then
  echo "WARN: canary not ready yet"
  exit 1
fi
echo "DEPLOY_OK"
