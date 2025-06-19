const backendURL = "http://localhost:8085";

let totalFloors = 0;
let elevator;
let currentFloor = 0;

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
    controls.innerHTML = `<span class="floor-number">Floor ${i}</span><button onclick="requestPickup(${i})">Request</button>`;
    floor.appendChild(controls);

    container.appendChild(floor);
  }

  elevator = document.createElement("div");
  elevator.className = "elevator";
  elevator.style.top = "";
  container.appendChild(elevator);

  currentFloor = 0;
  setElevatorToFloor(currentFloor);
  updateFloorDisplay(currentFloor);
  pollCurrentFloor();
}

function setElevatorToFloor(floor) {
  const targetFloorEl = document.querySelector(`[data-floor="${floor}"]`);
  if (!targetFloorEl) return;

  const containerTop = document.getElementById("buildingContainer").getBoundingClientRect().top;
  const targetTop = targetFloorEl.getBoundingClientRect().top;
  const offset = targetTop - containerTop;

  requestAnimationFrame(() => {
    elevator.style.top = `${offset + 10}px`;
  });

  currentFloor = floor;
  updateFloorDisplay(floor);
}

function updateFloorDisplay(floor) {
  const display = document.getElementById("currentFloorDisplay");
  if (display) {
    display.textContent = `${floor}`;
  }
}

// Called when a floor button is clicked
function requestPickup(floor) {
  fetch(`${backendURL}/pickup?floor=${floor}`)
    .then(() => {
      console.log(`Pickup requested from floor ${floor}`);
    })
    .catch(console.error);

  showDestinationInputBox(floor);
}

function showDestinationInputBox(pickupFloor) {
  setTimeout(() => {
    const floorElement = document.querySelector(`.floor[data-floor='${pickupFloor}']`);

    const existingBox = document.getElementById("destination-box");
    if (existingBox) existingBox.remove();

    const inputBox = document.createElement("div");
    inputBox.id = "destination-box";
    inputBox.innerHTML = `
      <label style="font-size: 13px; margin-bottom: 4px;">Enter destination:</label>
      <input type="number" id="destinationInput" placeholder="ex: 2" min="0" max="${totalFloors - 1}" style="width: 70px; padding: 4px;" />
      <button onclick="sendToDestination(${pickupFloor})" style="margin-top: 4px;">Go</button>
    `;

    floorElement.appendChild(inputBox);
  }, 1000);
}

function sendToDestination(pickupFloor) {
  const input = document.getElementById("destinationInput");
  const destination = parseInt(input.value);

  if (isNaN(destination) || destination < 0 || destination >= totalFloors) {
    alert("Please enter a valid floor.");
    return;
  }

  fetch(`${backendURL}/destination?floor=${destination}`)
    .then(() => {
      console.log(`Destination set to floor ${destination}`);
    })
    .catch(console.error);

  const inputBox = document.getElementById("destination-box");
  if (inputBox) inputBox.remove();

  const pickupEl = document.querySelector(`.floor[data-floor='${pickupFloor}']`);
  pickupEl.classList.add("pickup-highlight");

  setTimeout(() => {
    const dropEl = document.querySelector(`.floor[data-floor='${destination}']`);
    dropEl.classList.add("drop-highlight");

    setTimeout(() => {
      pickupEl.classList.remove("pickup-highlight");
      dropEl.classList.remove("drop-highlight");
    }, 1200);
  }, 2000);
}

function goToFloor() {
  const input = document.getElementById("directDestination");
  const floor = parseInt(input.value);

  if (isNaN(floor) || floor < 0 || floor >= totalFloors) {
    alert("Please enter a valid floor.");
    return;
  }

  fetch(`${backendURL}/pickup?floor=${floor}`)
    .then(() => console.log(`Manual pickup request to floor ${floor}`))
    .catch(console.error);

  input.value = "";
}

// Poll elevator status every 2s
function pollCurrentFloor() {
  setInterval(() => {
    fetch(`${backendURL}/status`)
      .then((res) => res.json())
      .then((data) => {
        setElevatorToFloor(data.floor);
      })
      .catch(console.error);
  }, 2000);
}
