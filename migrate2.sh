#!/bin/bash
set -e
B=/Users/riccardo.saponi/Workspace/IdeaProjects/quix/backend-springboot-template
cd "$B"

SRC=src/main/java/it/quix/nomecliente
ENTITY=libs/entity-domain/src/main/java/it/quix/nomecliente

echo "=== entity-domain: ports out ==="
[ -f $SRC/domain/port/out/EntityRepositoryOut.java ] && mv $SRC/domain/port/out/EntityRepositoryOut.java $ENTITY/domain/port/out/
[ -f $SRC/domain/port/out/HealthCheckOut.java ]      && mv $SRC/domain/port/out/HealthCheckOut.java $ENTITY/domain/port/out/

echo "=== entity-domain: use cases ==="
for f in $SRC/domain/usecase/*.java; do
  [ -f "$f" ] && mv "$f" "$ENTITY/domain/usecase/$(basename $f)"
done

echo "=== entity-domain: infrastructure ==="
find $SRC/infrastructure -name "*.java" 2>/dev/null | while read f; do
  REL="${f#$SRC/infrastructure/}"
  DEST="$ENTITY/infrastructure/$REL"
  mkdir -p "$(dirname $DEST)"
  mv "$f" "$DEST"
done

echo "=== app: resources ==="
[ -f src/main/resources/application.yml ] && mv src/main/resources/application.yml app/src/main/resources/application.yml
[ -f src/main/resources/db/changelog/db.changelog-master.yaml ] && \
  mv src/main/resources/db/changelog/db.changelog-master.yaml app/src/main/resources/db/changelog/db.changelog-master.yaml
[ -f "src/main/resources/db/changelog/changes/001-init-schema.yaml" ] && \
  mv "src/main/resources/db/changelog/changes/001-init-schema.yaml" "app/src/main/resources/db/changelog/changes/001-init-schema.yaml"

echo "=== app: tests ==="
for f in src/test/java/it/quix/nomecliente/domain/usecase/*.java; do
  [ -f "$f" ] && mv "$f" "app/src/test/java/it/quix/nomecliente/domain/usecase/$(basename $f)"
done
[ -f src/test/java/it/quix/nomecliente/fixtures/EntityTestFixtures.java ] && \
  mv src/test/java/it/quix/nomecliente/fixtures/EntityTestFixtures.java app/src/test/java/it/quix/nomecliente/fixtures/
[ -f src/test/resources/application-integration.yml ] && \
  mv src/test/resources/application-integration.yml app/src/test/resources/

echo "=== git add all ==="
git add -A

echo "=== DONE ==="
