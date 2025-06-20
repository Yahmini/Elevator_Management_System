// script.js
const backendURL = "http://localhost:8085";

let totalFloors = 0;
let elevator;
let currentFloor = 0;
const pickupRequestedFloors = new Set();
const destinationFloorsToHighlight = new Map();

function buildBuilding() {
  const container = document.getElementById("buildingContainer");
  container.innerHTML = "";
  totalFloors = parseInt(document.getElementById("floorInput").value);

  for (let i = totalFloors - 1; i >= 0; i--) {
    const floor = document.createElement("div");
    floor.className = "floor";
    floor.setAttribute("data-floor", i);

    const shaft = document.createElement("div");
    shaft.className = "lift-shaft";
    floor.appendChild(shaft);

    const controls = document.createElement("div");
    controls.className = "floor-controls";
    controls.innerHTML = `<span class="floor-number">Floor ${i}</span><button onclick="requestPickup(${i})">  </button>`;
    floor.appendChild(controls);

    container.appendChild(floor);
  }

  elevator = document.createElement("div");
  elevator.className = "elevator";
  container.appendChild(elevator);

  currentFloor = 0;
  setElevatorToFloor(currentFloor);
  updateFloorDisplay(currentFloor);
  pollCurrentFloor();
}

function requestPickup(floor) {
  if (pickupRequestedFloors.has(floor)) return;
  pickupRequestedFloors.add(floor);

  fetch(`${backendURL}/pickup?floor=${floor}`);
}

function showDestinationInputBox(floor) {
  const floorEl = document.querySelector(`.floor[data-floor='${floor}']`);
  if (!floorEl) return;

  const existing = document.getElementById("destination-box");
  if (existing) existing.remove();

  const inputBox = document.createElement("div");
  inputBox.id = "destination-box";
  inputBox.innerHTML = `
    <label style="font-size: 13px; margin-bottom: 4px;">Enter destination:</label>
    <input type="number" id="destinationInput" min="0" max="${totalFloors - 1}" style="width: 70px;" />
    <button onclick="sendToDestination(${floor})">Go</button>
  `;
  floorEl.appendChild(inputBox);

  floorEl.classList.add("pickup-highlight");
  floorEl.classList.remove("pickup-highlight");
}

function sendToDestination(pickupFloor) {
  const input = document.getElementById("destinationInput");
  const dest = parseInt(input.value);
  if (isNaN(dest) || dest < 0 || dest >= totalFloors) {
    alert("Invalid floor");
    return;
  }

  fetch(`${backendURL}/destination?floor=${dest}`);
  destinationFloorsToHighlight.set(dest, true);

  document.getElementById("destination-box")?.remove();
}

function setElevatorToFloor(floor) {
  const target = document.querySelector(`[data-floor="${floor}"]`);
  if (!target) return;

  const offset = target.offsetTop;
  elevator.style.top = `${offset + 10}px`;

  updateFloorDisplay(floor);

  if (pickupRequestedFloors.has(floor)) {
    pickupRequestedFloors.delete(floor);
    showDestinationInputBox(floor);
  }

  if (destinationFloorsToHighlight.has(floor)) {
    const el = document.querySelector(`.floor[data-floor="${floor}"]`);
    el?.classList.add("drop-highlight");
    destinationFloorsToHighlight.delete(floor);
    setTimeout(() => el?.classList.remove("drop-highlight"), 1500);
  }
}

function updateFloorDisplay(floor) {
  const display = document.getElementById("currentFloorDisplay");
  if (display) display.textContent = `${floor}`;
}

function goToFloor() {
  const input = document.getElementById("directDestination");
  const floor = parseInt(input.value);
  if (isNaN(floor) || floor < 0 || floor >= totalFloors) {
    alert("Enter a valid floor");
    return;
  }
  requestPickup(floor);
  input.value = "";
}

function pollCurrentFloor() {
  setInterval(() => {
    fetch(`${backendURL}/status`)
      .then(res => res.json())
      .then(data => {
        setElevatorToFloor(data.floor);
      });
  }, 1000);
}
