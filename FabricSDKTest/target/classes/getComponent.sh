if [ -d "./component" ]; then
  rm -rf component
fi

scp block@114.116.67.108:/home/block/gowork/src/github.com/hyperledger/fabric/examples/e2e_cli/component.tar ./

tar -xvf component.tar

rm component.tar
