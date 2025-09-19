GIT_HEAD_ARG = --build-arg=GIT_HEAD=$(shell git rev-parse HEAD)
export DOCKER_BUILDKIT = 1

# Set it to `--file=docker-compose-cluster.yaml` to run the acceptance tests in cluster mode
DOCKER_COMPOSE_ARGS ?=

.PHONY: clean
clean:
	rm -rf .env examples/geoserver-data/logs/

.PHONY: build
build: build-builder
	docker build $(GIT_HEAD_ARG) --target=runner --tag=camptocamp/mapfish_print core
	docker build $(GIT_HEAD_ARG) --target=tester --tag=mapfish_print_tester core
	docker build $(GIT_HEAD_ARG) --target=watcher --tag=mapfish_print_watcher core

.PHONY: build-builder
build-builder:
	# Required and not necessarily exists
	touch CI.asc

	docker build $(GIT_HEAD_ARG) --target=builder --tag=mapfish_print_builder .

.PHONY: checks
checks: build-builder
	mkdir --parent reports
	mkdir --parent core/build/resources/
	docker run --rm --user=$(shell id -u):$(shell id -g) \
		--volume=$(PWD)/core/src/:/src/core/src/:ro \
		--volume=$(PWD)/reports/:/src/core/build/reports/ \
		--volume=$(PWD)/core/build/resources/:/src/core/build/resources/ \
		mapfish_print_builder \
		gradle --parallel spotbugsMain checkstyleMain spotbugsTest checkstyleTest

.PHONY: tests
tests: build-builder
	mkdir --parent core/build/reports/
	mkdir --parent core/build/resources/
	mkdir --parent core/build/scripts/
	mkdir --parent examples/build/resources/test/
	docker run --rm --user=$(shell id -u):$(shell id -g) \
		--volume=$(PWD)/core/src/:/src/core/src/:ro \
		--volume=$(PWD)/core/build/reports/:/src/core/build/reports/ \
		--volume=$(PWD)/core/build/resources/:/src/core/build/resources/ \
		--volume=$(PWD)/core/build/scripts/:/src/core/build/scripts/ \
		--volume=$(PWD)/core/src/test/:/src/core/src/test/ \
		mapfish_print_builder \
		gradle --parallel --exclude-task=:core:spotbugsMain --exclude-task=:core:checkstyleMain \
			--exclude-task=:core:spotbugsTest --exclude-task=:core:checkstyleTest \
			--info :core:test :core:testCli

.PHONY: acceptance-tests-up
acceptance-tests-up: build .env
	# Required to avoid root ownership of reports folder
	mkdir -p examples/build/reports/ || true
	docker compose $(DOCKER_COMPOSE_ARGS) up --detach

.PHONY: acceptance-tests-run
acceptance-tests-run: .env
	# Wait for GeoServer to be started
	# Wait for GeoServer to be ready (up to 2 minutes)
	docker compose exec $(DOCKER_COMPOSE_ARGS) -T print sh -c 'for i in $(shell seq 1 24); do curl --fail http://geoserver:8080/geoserver/web/ && exit 0; sleep 5; done; exit 1'

	git status
	ls -l core/src/test/resources/map-data/test-data/simple-squares-geojson.json

	docker compose exec -T print curl --fail http://geoserver:8080/geoserver/www/map-data/test-data/simple-squares-geojson.json

	docker compose $(DOCKER_COMPOSE_ARGS) exec -T tests gradle \
		--exclude-task=:core:spotbugsMain --exclude-task=:core:checkstyleMain \
		--exclude-task=:core:spotbugsTest --exclude-task=:core:checkstyleTest --exclude-task=:core:testCLI \
		:examples:integrationTest
	ci/check-fonts
	ci/validate-container

.PHONY: acceptance-tests-down
acceptance-tests-down: .env
	docker compose $(DOCKER_COMPOSE_ARGS) down || true

.PHONY: dist
dist: build-builder
	mkdir --parent core/build
	rm -rf core/build/libs core/build/distributions
	docker run --rm --user=$(shell id -u):$(shell id -g) \
		--volume=$(PWD)/core/build:/src/core/build2/:rw mapfish_print_builder \
		cp -r /src/core/build/libs /src/core/build/distributions /src/core/build2/

.env:
	echo "USER_ID=$(shell id -u):$(shell id -g)" > $@
