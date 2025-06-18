let totalFloors = 0;
let elevator;
let currentFloor = 0;

function buildBuilding() {
  const container = document.getElementById("buildingContainer");
  container.innerHTML = "";
  totalFloors = parseInt(document.getElementById("floorInput").value);

  for (let i = totalFloors; i >= 0; i--) {
    const floor = document.createElement("div");
    floor.className = "floor";
    floor.setAttribute("data-floor", i);

    const shaft = document.createElement("div");
    shaft.className = "lift-shaft";
    floor.appendChild(shaft);

    const controls = document.createElement("div");
    controls.className = "floor-controls";
    controls.innerHTML = `<span class="floor-number">Floor ${i}</span><button onclick="moveElevator(${i})">Request</button>`;
    floor.appendChild(controls);

    container.appendChild(floor);
  }

  elevator = document.createElement("div");
  elevator.className = "elevator";

  const groundFloor = container.querySelector(`[data-floor="0"] .lift-shaft`);
  groundFloor.appendChild(elevator);
  currentFloor = 0;
}

function updateElevatorPosition(floor) {
  const shaft = document.querySelector(`[data-floor="${floor}"] .lift-shaft`);
  if (shaft) {
    shaft.appendChild(elevator);
  }
  currentFloor = floor;
}

function moveElevator(targetFloor) {
  updateElevatorPosition(targetFloor);

  setTimeout(() => {
    const floorElement = document.querySelector(`.floor[data-floor='${targetFloor}']`);

    const existingBox = document.getElementById("destination-box");
    if (existingBox) existingBox.remove();

    const inputBox = document.createElement("div");
    inputBox.id = "destination-box";
    inputBox.innerHTML = `
      <label style="font-size: 13px; margin-bottom: 4px;">Enter destination:</label>
      <input type="number" id="destinationInput" placeholder="ex: 2" min="0" max="${totalFloors - 1}" style="width: 70px; padding: 4px;" />
      <button onclick="sendToDestination(${targetFloor})" style="margin-top: 4px;">Go</button>
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

  document.getElementById("destination-box").remove();

  const pickupEl = document.querySelector(`.floor[data-floor='${pickupFloor}']`);
  pickupEl.classList.add("pickup-highlight");

  setTimeout(() => {
    moveElevator(destination);

    setTimeout(() => {
      const dropEl = document.querySelector(`.floor[data-floor='${destination}']`);
      dropEl.classList.add("drop-highlight");

      setTimeout(() => {
        pickupEl.classList.remove("pickup-highlight");
        dropEl.classList.remove("drop-highlight");
      }, 1200);
    }, 1000);
  }, 500);
}
