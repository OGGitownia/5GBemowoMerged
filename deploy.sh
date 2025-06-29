SERVER_USER="root"
SERVER_IP="213.181.122.2"
SERVER_PORT="52220"

PROJECT_DIR="."
TAR_FILE="5gbemowo.tar.gz"
REMOTE_PATH="/root"

echo "Pakuję katalog do $TAR_FILE..."
tar --exclude=deploy.sh -czf $TAR_FILE .

echo "Wysyłam $TAR_FILE na serwer $SERVER_IP..."
scp -P $SERVER_PORT $TAR_FILE ${SERVER_USER}@${SERVER_IP}:${REMOTE_PATH}

echo "Logowanie do serwera i uruchamianie kontenerów..."
ssh -p $SERVER_PORT ${SERVER_USER}@${SERVER_IP} << EOF
  cd $REMOTE_PATH
  echo "Usuwam stary katalog projektu..."
  rm -rf 5GBemowoMerged

  echo "Rozpakowuję nowy projekt..."
  mkdir -p 5GBemowoMerged && tar -xzf $TAR_FILE -C 5GBemowoMerged
  cd 5GBemowoMerged

  echo "Buduję i uruchamiam kontenery..."
  docker compose up --build -d

  echo "Deployment zakończony!"
EOF


# To run that chmod +x deploy.sh and than ./deploy.sh

