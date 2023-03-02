GIT_HEAD_ARG = --build-arg=GIT_HEAD=$(shell git rev-parse HEAD)
export DOCKER_BUILDKIT = 1

.PHONY: build
build:
	# Required and not nesseerly exists
	touch CI.asc

	docker build $(GIT_HEAD_ARG) --target=builder --tag=mapfish_print_builder .
	docker build $(GIT_HEAD_ARG) .

	docker build $(GIT_HEAD_ARG) --target=runner --tag=camptocamp/mapfish_print core
	docker build $(GIT_HEAD_ARG) --target=tester --tag=mapfish_print_tester core
	docker build $(GIT_HEAD_ARG) --target=watcher --tag=mapfish_print_watcher core

.PHONY: acceptance-tests-up
acceptance-tests-up:
	docker-compose down --remove-orphan

	mkdir /tmp/geoserver-data || true
	docker run --rm --volume=/tmp/geoserver-data:/mnt/geoserver_datadir camptocamp/geoserver \
		bash -c 'rm -rf /mnt/geoserver_datadir/*'
	mkdir /tmp/geoserver-data/www
	cp -r examples/geoserver-data/* /tmp/geoserver-data/
	cp -r core/src/test/resources/map-data/* /tmp/geoserver-data/www/

	docker-compose up -d

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
