const backendURL = "http://localhost:8085";

let totalFloors = 0;
let elevator;
let currentFloor = 0;
let awaitingInput = false;
let lastWaitingFloor = -1;

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
    controls.innerHTML = `<span class="floor-number">Floor ${i}</span><button onclick="requestPickup(${i})"></button>`;
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
    const button = document.querySelector(`[data-floor="${floor}"] button`);
    if (button) {
      button.classList.add("request-pending");
    }
  
    fetch(`${backendURL}/pickup?floor=${floor}`);
  }
  

function sendToDestination() {
    const input = document.getElementById("directDestination");
    const dest = parseInt(input.value);
    const pickupFloor = parseInt(input.getAttribute("data-pickup"));
  
    if (isNaN(dest) || dest < 0 || dest >= totalFloors) {
      alert("Enter a valid floor");
      return;
    }
  
    fetch(`${backendURL}/destination?floor=${dest}`).then(() => {
      input.value = "";
      input.disabled = true;
      document.getElementById("goBtn").disabled = true;
      awaitingInput = false;
      lastWaitingFloor = -1; // ✅ so it can trigger again later
    });
  }
  
  

function showControlInputBox(floor) {
  const input = document.getElementById("directDestination");
  const btn = document.getElementById("goBtn");
  input.disabled = false;
  btn.disabled = false;
  input.setAttribute("data-pickup", floor);
  awaitingInput = true;
}

function setElevatorToFloor(floor) {
    const target = document.querySelector(`[data-floor="${floor}"]`);
    if (!target) return;
  
    const offset = target.offsetTop;
    elevator.style.top = `${offset + 10}px`;
    updateFloorDisplay(floor);
  
    // Remove request-pending if present (pickup done)
    const pickupButton = target.querySelector("button");
    if (pickupButton) {
      pickupButton.classList.remove("request-pending");
    }
  
    // If drop highlight needed
    if (target.classList.contains("drop-highlight")) return;
  
    // Check if it's a drop-off floor
    if (!awaitingInput && lastWaitingFloor !== floor) {
      target.classList.add("drop-highlight");
      setTimeout(() => {
        target.classList.remove("drop-highlight");
      }, 2000);
    }
  }
  

function updateFloorDisplay(floor) {
  const display = document.getElementById("currentFloorDisplay");
  if (display) display.textContent = `${floor}`;
}

function pollCurrentFloor() {
  setInterval(() => {
    fetch(`${backendURL}/status`)
      .then(res => res.json())
      .then(data => {
        setElevatorToFloor(data.floor);
        currentFloor = data.floor;

        if (data.waiting && !awaitingInput && lastWaitingFloor !== data.floor) {
          showControlInputBox(data.floor);
          lastWaitingFloor = data.floor;
        }
      });
  }, 1000);
}
