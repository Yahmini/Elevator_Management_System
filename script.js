const backendURL = "http://localhost:8085";

let totalFloors = 0;
let elevators = {
  A: { element: null, currentFloor: 0, awaitingInput: false, lastWaitingFloor: -1, inMaintenance: false },
  B: { element: null, currentFloor: 0, awaitingInput: false, lastWaitingFloor: -1, inMaintenance: false }
};

function buildBuilding() {
  const container = document.getElementById("buildingContainer");
  container.innerHTML = "";
  totalFloors = parseInt(document.getElementById("floorInput").value);

  for (let i = totalFloors; i >= 0; i--) {
    const floor = document.createElement("div");
    floor.className = "floor";
    floor.setAttribute("data-floor", i);

    const shaftA = document.createElement("div");
    shaftA.className = "lift-shaft shaft-A";

    const shaftB = document.createElement("div");
    shaftB.className = "lift-shaft shaft-B";

    floor.appendChild(shaftA);
    floor.appendChild(shaftB);

    const controls = document.createElement("div");
    controls.className = "floor-controls";
    controls.innerHTML = `<span class="floor-number">Floor ${i}</span><button onclick="requestPickup(${i})"></button>`;
    floor.appendChild(controls);

    container.appendChild(floor);
  }

  elevators.A.element = document.createElement("div");
  elevators.A.element.className = "elevator elevator-A";
  container.appendChild(elevators.A.element);

  elevators.B.element = document.createElement("div");
  elevators.B.element.className = "elevator elevator-B";
  container.appendChild(elevators.B.element);

  setupMaintenanceToggles();
  pollElevators();
}

function requestPickup(floor) {
  const button = document.querySelector(`[data-floor="${floor}"] button`);
  if (button) button.classList.add("request-pending");

  fetch(`${backendURL}/pickup?floor=${floor}`)
    .then(_ => console.log("Pickup sent", floor));
}

function sendToDestination(elevatorId) {
  const input = document.getElementById(`directDestination${elevatorId}`);
  const btn = document.getElementById(`goBtn${elevatorId}`);
  const dest = parseInt(input.value);

  if (isNaN(dest) || dest < 0 || dest > totalFloors) {
    alert("Enter a valid floor");
    return;
  }

  fetch(`${backendURL}/destination?elevator=${elevatorId}&floor=${dest}`).then(() => {
    input.value = "";
    input.disabled = true;
    btn.disabled = true;
    elevators[elevatorId].awaitingInput = false;
    elevators[elevatorId].lastWaitingFloor = -1;
  });
}

function showControlInputBox(elevatorId, floor) {
  const input = document.getElementById(`directDestination${elevatorId}`);
  const btn = document.getElementById(`goBtn${elevatorId}`);

  input.disabled = false;
  btn.disabled = false;
  elevators[elevatorId].awaitingInput = true;
}

function setElevatorToFloor(elevatorId, floor) {
  const elevatorData = elevators[elevatorId];
  const target = document.querySelector(`[data-floor="${floor}"]`);
  if (!target || !elevatorData.element) return;

  const floorTop = target.offsetTop;
  elevatorData.element.style.top = `${floorTop + 10}px`;

  const pickupBtn = target.querySelector("button");
  if (pickupBtn?.classList.contains("request-pending")) {
    target.classList.add("pickup-highlight");
    pickupBtn.classList.remove("request-pending");
    setTimeout(() => target.classList.remove("pickup-highlight"), 2000);
  }

  if (!elevatorData.awaitingInput && elevatorData.lastWaitingFloor !== floor) {
    target.classList.add("drop-purple");
    setTimeout(() => target.classList.remove("drop-purple"), 2000);
  }
}

function pollElevators() {
  setInterval(() => {
    fetch(`${backendURL}/status`)
      .then(res => res.json())
      .then(data => {
        ["A", "B"].forEach(id => {
          let state = data[`elevator${id}`];
          if (!state) return;

          elevators[id].currentFloor = state.floor;
          elevators[id].inMaintenance = state.inMaintenance;

          setElevatorToFloor(id, state.floor);

          const input = document.getElementById(`directDestination${id}`);
          const btn = document.getElementById(`goBtn${id}`);
          const toggle = document.getElementById(`maintToggle${id}`);

          if (state.waiting && !elevators[id].awaitingInput && !state.inMaintenance) {
            input.disabled = false;
            btn.disabled = false;
            elevators[id].awaitingInput = true;
          }

          // Reflect maintenance toggle state
          if (toggle) toggle.checked = !state.inMaintenance;

          // Gray out elevator in maintenance
          elevators[id].element.style.opacity = state.inMaintenance ? "0.3" : "1";
          input.disabled = state.inMaintenance;
          btn.disabled = state.inMaintenance;
        });
      });
  }, 1000);
}

// Maintenance toggle
function setupMaintenanceToggles() {
  const ctrlDiv = document.getElementById("maintenanceControls");
  ctrlDiv.innerHTML = `<h3>Maintenance Mode</h3>`;
  ["A", "B"].forEach(id => {
    const toggleBox = document.createElement("div");
    toggleBox.innerHTML = `
      <label>Elevator ${id}
        <input type="checkbox" id="maintToggle${id}" onchange="toggleMaintenance('${id}')">
        <span style="font-size: 14px;">&nbsp;Enable</span>
      </label><br/>`;
    ctrlDiv.appendChild(toggleBox);
  });
}

function toggleMaintenance(id) {
  const checked = document.getElementById(`maintToggle${id}`).checked;
  const mode = checked ? "off" : "on"; // inverse because checked=true means active (not maintenance)
  fetch(`${backendURL}/maintenance?elevator=${id}&mode=${mode}`)
    .then(() => console.log(`Elevator ${id} maintenance ${mode}`));
}
