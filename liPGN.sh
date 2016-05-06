#!/bin/bash

exec java -cp liPGN.jar                                                   \
  -client                                                                 \
  -Xmx500m                                                                \
  -XX:+PerfDisableSharedMem                                               \
  -Dstorage.diskCache.bufferSize=50                                       \
  -Dstorage.wal.maxSize=15                                                \
  -Dstorage.keepOpen=true                                                 \
  -Ddb.pool.max=3                                                         \
  fr.tedoldi.lichess.game.retriever.main $*
