GIT_HEAD_ARG = --build-arg=GIT_HEAD=$(shell git rev-parse HEAD)
export DOCKER_BUILDKIT = 1

.PHONY: build
build: build-builder
	# Required and not necessarily exists
	touch CI.asc

	docker build $(GIT_HEAD_ARG) --target=runner --tag=camptocamp/mapfish_print core
	docker build $(GIT_HEAD_ARG) --target=tester --tag=mapfish_print_tester core
	docker build $(GIT_HEAD_ARG) --target=watcher --tag=mapfish_print_watcher core

.PHONY: build-builder
build-builder:
	docker build $(GIT_HEAD_ARG) --target=builder --tag=mapfish_print_builder .

.PHONY: checks
checks: build-builder
	mkdir --parent reports
	docker run --rm --user=$(shell id -u):$(shell id -g) \
		--volume=$(PWD)/core/src/:/src/core/src/:ro \
		--volume=$(PWD)/reports/:/src/core/build/reports/ \
		mapfish_print_builder \
		gradle --parallel :core:spotbugsMain :core:checkstyleMain :core:violations

.PHONY: tests
tests: build-builder
	mkdir --parent core/build/reports/
	mkdir --parent core/build/resources/
	mkdir --parent core/build/scripts/
	docker run --rm --user=$(shell id -u):$(shell id -g) \
		--volume=$(PWD)/core/src/:/src/core/src/:ro \
		--volume=$(PWD)/core/build/reports/:/src/core/build/reports/ \
		--volume=$(PWD)/core/build/resources/:/src/core/build/resources/ \
		--volume=$(PWD)/core/build/scripts/:/src/core/build/scripts/ \
		--volume=$(PWD)/core/src/test/:/src/core/src/test/:ro \
		mapfish_print_builder \
		gradle --parallel --exclude-task=:core:spotbugsMain --exclude-task=:core:checkstyleMain --exclude-task=:core:violations \
			--exclude-task=:core:spotbugsTest --exclude-task=:core:checkstyleTest \
			:core:test :core:testCli

.PHONY: acceptance-tests-up
acceptance-tests-up: build .env
	docker-compose down --remove-orphan

	mkdir /tmp/geoserver-data || true
	docker run --rm --volume=/tmp/geoserver-data:/mnt/geoserver_datadir camptocamp/geoserver \
		bash -c 'rm -rf /mnt/geoserver_datadir/*'
	mkdir /tmp/geoserver-data/www
	cp -r examples/geoserver-data/* /tmp/geoserver-data/
	cp -r core/src/test/resources/map-data/* /tmp/geoserver-data/www/

	# Required to avoid root ownership of reports folder
	mkdir -p examples/build/reports/ || true
	docker-compose up --detach

.PHONY: acceptance-tests-run
acceptance-tests-run: .env
	docker-compose exec -T tests gradle \
		--exclude-task=:core:spotbugsMain --exclude-task=:core:checkstyleMain --exclude-task=:core:violations \
		--exclude-task=:core:spotbugsTest --exclude-task=:core:checkstyleTest --exclude-task=:core:testCLI \
		:examples:integrationTest
	ci/check-fonts
	ci/validate-container

.PHONY: acceptance-tests-down
acceptance-tests-down: .env
	docker-compose down || true
	docker run --rm --volume=/tmp/geoserver-data:/mnt/geoserver_datadir camptocamp/geoserver \
		bash -c 'rm -rf /mnt/geoserver_datadir/*'
	rmdir /tmp/geoserver-data

.env:
	echo "USER_ID=$(shell id -u):$(shell id -g)" > $@
