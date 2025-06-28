const backendURL = "http://localhost:8085";

let totalFloors = 0;

elevators = {
  A: { element: null, currentFloor: 0, awaitingInput: false, lastWaitingFloor: -1, inMaintenance: false, previousFloor: -1, inputTimeout: null },
  B: { element: null, currentFloor: 0, awaitingInput: false, lastWaitingFloor: -1, inMaintenance: false, previousFloor: -1, inputTimeout: null }
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

    elevators.A.currentFloor = 0;
    elevators.B.currentFloor = 0;
    setElevatorToFloor("A", 0);
    setElevatorToFloor("B", 0);

    container.appendChild(floor);
  }

  elevators.A.element = document.createElement("div");
  elevators.A.element.className = "elevator elevator-A";
  elevators.A.element.innerHTML = `
    <div class="elevator-door left"></div>
    <div class="elevator-door right"></div>
  `;
  container.appendChild(elevators.A.element);

  elevators.B.element = document.createElement("div");
  elevators.B.element.className = "elevator elevator-B";
  elevators.B.element.innerHTML = `
    <div class="elevator-door left"></div>
    <div class="elevator-door right"></div>
  `;
  container.appendChild(elevators.B.element);

  setupMaintenanceToggles();
  pollElevators();
}

function requestPickup(floor) {
  const button = document.querySelector(`[data-floor="${floor}"] button`);
  if (button) button.classList.add("request-pending");

  fetch(`${backendURL}/pickup?floor=${floor}`).then(_ => console.log("Pickup sent", floor));
}

function sendToDestination(elevatorId) {
  const input = document.getElementById(`directDestination${elevatorId}`);
  const btn = document.getElementById(`goBtn${elevatorId}`);
  const dest = parseInt(input.value);

  if (isNaN(dest) || dest < 0 || dest > totalFloors) {
    alert("Enter a valid floor");
    return;
  }
  clearTimeout(elevators[elevatorId].inputTimeout);

  fetch(`${backendURL}/destination?elevator=${elevatorId}&floor=${dest}`).then(() => {
    input.value = "";
    input.disabled = true;
    btn.disabled = true;
    elevators[elevatorId].awaitingInput = false;
    elevators[elevatorId].lastWaitingFloor = -1;
  });
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
          const state = data[`elevator${id}`];
          const elData = elevators[id];
          if (!state || !elData.element) return;

          const prevFloor = elData.currentFloor;
          elData.currentFloor = state.floor;
          elData.inMaintenance = state.inMaintenance;

          setElevatorToFloor(id, state.floor);

          const input = document.getElementById(`directDestination${id}`);
          const btn = document.getElementById(`goBtn${id}`);
          const toggle = document.getElementById(`maintToggle${id}`);


          if (state.waiting && !elData.awaitingInput && !state.inMaintenance) {
            input.disabled = false;
            btn.disabled = false;
            elData.awaitingInput = true;
            clearTimeout(elData.inputTimeout);
            elData.inputTimeout = setTimeout(() => {
            input.disabled = true;
            btn.disabled = true;
            elData.awaitingInput = false;
            input.value = "";


            fetch(`${backendURL}/timeout?elevator=${id}`);
            }, 8000);


          }

          if (toggle) toggle.checked = !state.inMaintenance;
          elData.element.style.opacity = state.inMaintenance ? "0.3" : "1";
          input.disabled = state.inMaintenance;
          btn.disabled = state.inMaintenance;


          if (state.waiting && elData.previousFloor !== state.floor) {

            elData.element.classList.remove("open");

            clearTimeout(elData.doorTimer);


            elData.doorTimer = setTimeout(() => {
              elData.element.classList.add("open");

              setTimeout(() => {
                elData.element.classList.remove("open");
              }, 3000);
            }, 2000);
          }

          elData.previousFloor = state.floor;
        });

        const statusPanel = document.getElementById("elevatorStatus");
        if (statusPanel) {
          statusPanel.innerText = `Elevator A: Floor ${data.elevatorA.floor} | Elevator B: Floor ${data.elevatorB.floor}`;
        }
      });
  }, 1000);
}

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
  const mode = checked ? "off" : "on";
  fetch(`${backendURL}/maintenance?elevator=${id}&mode=${mode}`)
    .then(() => console.log(`Elevator ${id} maintenance ${mode}`));
}
