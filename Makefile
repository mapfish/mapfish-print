GIT_HEAD_ARG = --build-arg=GIT_HEAD=$(shell git rev-parse HEAD)
export DOCKER_BUILDKIT = 1

.PHONY: build
build: build-builder
	# Required and not nesseerly exists
	touch CI.asc

	docker build $(GIT_HEAD_ARG) --target=runner --tag=camptocamp/mapfish_print core
	docker build $(GIT_HEAD_ARG) --target=tester --tag=mapfish_print_tester core
	docker build $(GIT_HEAD_ARG) --target=watcher --tag=mapfish_print_watcher core

.PHONY: build-builder
build-builder:
	docker build $(GIT_HEAD_ARG) --target=builder --tag=mapfish_print_builder .

.PHONY: checks
checks: spotbugs violations

# --volume=$(PWD):/src mapfish_print_builder
.PHONY: spotbugs
spotbugs: build-builder
	docker run --rm \
		mapfish_print_builder gradle :core:spotbugsMain

.PHONY: violations
violations: build-builder
	docker run --rm \
		mapfish_print_builder gradle :core:violations


.PHONY: profile
profile: build-builder ## Profile a task, the result will be in build/reports/profile
	docker stop print-builder || true
	docker run --rm --detach --name=print-builder mapfish_print_builder tail --follow /dev/null

	# Change this line to run the task you want to profile
	docker exec print-builder gradle --profile :core:test

	# Get the profile
	rm -rf build/reports/ || true
	mkdir -p build/
	docker cp print-builder:/src/build/reports/ build/
	
	docker stop print-builder

.PHONY: tests
tests: build-builder
	docker stop print-builder || true
	docker run --rm --detach --name=print-builder mapfish_print_builder tail --follow /dev/null

	docker exec print-builder gradle :core:test

	# Get the result
	mkdir -p core/build/resources/test/org/mapfish/print/
	docker cp print-builder:/src/core/build/resources/test/org/mapfish/print/ core/build/resources/test/org/mapfish/print/

	docker stop print-builder

.PHONY: acceptance-tests-up
acceptance-tests-up: build
	docker-compose down --remove-orphan

	mkdir /tmp/geoserver-data || true
	docker run --rm --volume=/tmp/geoserver-data:/mnt/geoserver_datadir camptocamp/geoserver \
		bash -c 'rm -rf /mnt/geoserver_datadir/*'
	mkdir /tmp/geoserver-data/www
	cp -r examples/geoserver-data/* /tmp/geoserver-data/
	cp -r core/src/test/resources/map-data/* /tmp/geoserver-data/www/

	USER_ID=$(shell id -u):$(shell id -g) docker-compose up --detach

.PHONY: acceptance-tests-run
acceptance-tests-run:
	docker-compose exec -T tests gradle :examples:integrationTest
	ci/check-fonts
	ci/validate-container

.PHONY: acceptance-tests-down
acceptance-tests-down:
	docker-compose down || true
	docker run --rm --volume=/tmp/geoserver-data:/mnt/geoserver_datadir camptocamp/geoserver \
		bash -c 'rm -rf /mnt/geoserver_datadir/*'
	rmdir /tmp/geoserver-data
