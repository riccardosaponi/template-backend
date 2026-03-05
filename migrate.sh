#!/bin/bash
set -e
B=/Users/riccardo.saponi/Workspace/IdeaProjects/quix/backend-springboot-template
cd "$B"

SRC=src/main/java/it/quix/nomecliente
APP=app/src/main/java/it/quix/nomecliente
SHARED=libs/shared-core/src/main/java/it/quix/nomecliente
ENTITY=libs/entity-domain/src/main/java/it/quix/nomecliente

echo "=== entity-domain: DTOs ==="
git mv $SRC/domain/ddd/dto/CreateEntityRequestDTO.java $ENTITY/domain/ddd/dto/
git mv $SRC/domain/ddd/dto/EntityDTO.java               $ENTITY/domain/ddd/dto/
git mv $SRC/domain/ddd/dto/UpdateEntityRequestDTO.java  $ENTITY/domain/ddd/dto/

echo "=== entity-domain: entity + enumeration ==="
git mv $SRC/domain/ddd/entity/Entity.java                     $ENTITY/domain/ddd/entity/
git mv $SRC/domain/ddd/enumeration/EntitySortField.java       $ENTITY/domain/ddd/enumeration/

echo "=== entity-domain: ports in ==="
git mv $SRC/domain/port/in/CreateEntityIn.java  $ENTITY/domain/port/in/
git mv $SRC/domain/port/in/DeleteEntityIn.java  $ENTITY/domain/port/in/
git mv $SRC/domain/port/in/GetEntityIn.java     $ENTITY/domain/port/in/
git mv $SRC/domain/port/in/HealthCheckIn.java   $ENTITY/domain/port/in/
git mv $SRC/domain/port/in/ListEntitiesIn.java  $ENTITY/domain/port/in/
git mv $SRC/domain/port/in/UpdateEntityIn.java  $ENTITY/domain/port/in/

echo "=== entity-domain: ports out ==="
mv $SRC/domain/port/out/EntityRepositoryOut.java  $ENTITY/domain/port/out/
mv $SRC/domain/port/out/HealthCheckOut.java       $ENTITY/domain/port/out/

echo "=== entity-domain: use cases ==="
for f in $SRC/domain/usecase/*.java; do mv "$f" "$ENTITY/domain/usecase/$(basename $f)"; done

echo "=== entity-domain: infrastructure ==="
find $SRC/infrastructure -name "*.java" | while read f; do
  REL="${f#$SRC/infrastructure/}"
  DEST="$ENTITY/infrastructure/$REL"
  mkdir -p "$(dirname $DEST)"
  mv "$f" "$DEST"
done

echo "=== app: resources ==="
mv src/main/resources/application.yml app/src/main/resources/application.yml
mv src/main/resources/db/changelog/db.changelog-master.yaml \
   app/src/main/resources/db/changelog/db.changelog-master.yaml
mv "src/main/resources/db/changelog/changes/001-init-schema.yaml" \
   "app/src/main/resources/db/changelog/changes/001-init-schema.yaml"

echo "=== app: tests ==="
for f in src/test/java/it/quix/nomecliente/domain/usecase/*.java; do
  mv "$f" "app/src/test/java/it/quix/nomecliente/domain/usecase/$(basename $f)"
done
mv src/test/java/it/quix/nomecliente/fixtures/EntityTestFixtures.java \
   app/src/test/java/it/quix/nomecliente/fixtures/
mv src/test/resources/application-integration.yml app/src/test/resources/

echo "=== git add all ==="
git add -A

echo "=== ALL DONE ==="
