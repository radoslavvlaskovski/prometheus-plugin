#!/usr/bin/env bash

install_node_exporter_ubuntu () {
    wget https://github.com/prometheus/node_exporter/releases/download/v0.15.2/node_exporter-0.15.2.linux-amd64.tar.gz
    if tar -xvzf node_exporter-0.15.2.linux-amd64.tar.gz
    then
        screen -S node_exporter -d -m node_exporter-0.15.2.linux-amd64/node_exporter
    else
        echo "error - no such Directory or file"
    fi
}



