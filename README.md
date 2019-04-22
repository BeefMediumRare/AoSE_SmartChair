# AoSE SmartChair
This repo implements the path planning algorithm described in `Kooperative Multi-Roboter-Wegplanung durch heuristische Prioritätsanpassung` using agent oriented software engineering. The goal of this project is to allow  a group of office chairs (`CaDS Smart Chairs`) to steer themself.

## Requirements
`Java 12` installed on your system

---
This repo holds multiple sub-projects.

## CoDy Agent
The actual path planning algorithm; and the communication and scheduling using `JADE`.

## MapBuilder
A GUI to open, save and create 2D gridworlds that contain static obstacles, start and end positions of robots. The resulting `.aose` file can be used by the simulation or deployment.

## Simulation
A simple simulation that reads `.aose` files and provides some envirement for the agents. It also has a GUI that allows to replay the simulations. If a folder `logs` exist next to the `Simulation_v1.0.0.jar`, `csv` files will be created containing data collected while simulating.

## Deployment
TBD