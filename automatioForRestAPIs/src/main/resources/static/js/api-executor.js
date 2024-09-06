document.addEventListener('DOMContentLoaded', function () {
    const executeApiBtn = document.getElementById('execute-api-btn');
    const apiResponseDiv = document.getElementById('api-response');
    const testCaseListDiv = document.getElementById('test-case-list');
    const spinner = document.createElement('div');
    spinner.className = 'spinner-border text-primary mt-3';
    spinner.style.display = 'none';
    spinner.role = 'status';
    spinner.innerHTML = '<span class="visually-hidden">Loading...</span>';
    apiResponseDiv.parentNode.insertBefore(spinner, apiResponseDiv);

    // Load test cases from localStorage
    const storedTestCases = JSON.parse(localStorage.getItem('generatedTestCases')) || [];
    displayTestCasesSidebar(storedTestCases);

    executeApiBtn.addEventListener('click', function () {
        const baseUrl = document.getElementById('baseUrl').value.trim();
        const endpoint = document.getElementById('endpoint').value.trim();
        const method = document.getElementById('method').value;
        const headers = JSON.parse(document.getElementById('headers').value || '{}');
        const body = document.getElementById('body').value;

        if (!baseUrl) {
            apiResponseDiv.innerHTML = `<h4>Error:</h4><pre>Please provide a base URL.</pre>`;
            return;
        }

        // Show spinner when starting the fetch request
        spinner.style.display = 'block';
        apiResponseDiv.innerHTML = '';  // Clear previous response

        fetch('/api/executor/execute', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                baseUrl: baseUrl,
                endpoint: endpoint,
                method: method,
                headers: headers,
                body: body
            })
        })
        .then(response => response.text())  // Ensure response is handled as text
        .then(data => {
            spinner.style.display = 'none';  // Hide spinner on successful response
            apiResponseDiv.innerHTML = `<h4>Response:</h4><pre>${escapeHtml(data)}</pre>`;
        })
        .catch(error => {
            spinner.style.display = 'none';  // Hide spinner on error
            apiResponseDiv.innerHTML = `<h4>Error:</h4><pre>${escapeHtml(error.message)}</pre>`;
        });
    });

    function displayTestCasesSidebar(testCases) {
        testCaseListDiv.innerHTML = '';

        testCases.forEach((tc, index) => {
            const listItem = document.createElement('a');
            listItem.href = '#';
            listItem.className = 'list-group-item list-group-item-action';
            listItem.innerHTML = `
                <strong>${decodeURIComponent(tc.endpoint.replace(/\+/g, ' '))}</strong> (${decodeURIComponent(tc.method.replace(/\+/g, ' '))})
                <i class="fas fa-chevron-down float-end" data-bs-toggle="collapse" data-bs-target="#collapse-${index}"></i>
            `;

            const collapseDiv = document.createElement('div');
            collapseDiv.id = `collapse-${index}`;
            collapseDiv.className = 'collapse mt-2';

            collapseDiv.innerHTML = `
                <div class="card card-body">
                    <p><strong>Description:</strong> ${decodeURIComponent(tc.description || 'No description available.').replace(/\+/g, ' ')}</p>
                    <p><strong>Steps:</strong><br>${tc.testCaseDetails ? tc.testCaseDetails.map(detail => detail.steps ? detail.steps.map(step => decodeURIComponent(step).replace(/\+/g, ' ')).join('<br>') : 'No steps provided').join('<br><br>') : 'No steps provided.'}</p>
                    <p><strong>Keywords:</strong> ${tc.testCaseDetails ? tc.testCaseDetails.map(detail => detail.keywords ? detail.keywords.map(keyword => decodeURIComponent(keyword).replace(/\+/g, ' ')).join(', ') : 'None').join(', ') : 'None'}</p>
                </div>
            `;

            listItem.addEventListener('click', function () {
                let baseUrl = document.getElementById('baseUrl').value.trim();

                if (!baseUrl) {
                    baseUrl = prompt("Please enter the base URL:");
                    if (baseUrl) {
                        document.getElementById('baseUrl').value = baseUrl;
                    } else {
                        alert("Base URL is required to proceed.");
                        return;
                    }
                }

                // Decode the endpoint and set it in the endpoint input field
                const endpoint = decodeURIComponent(tc.endpoint.replace(/\+/g, ' '));
                document.getElementById('endpoint').value = endpoint;

                document.getElementById('method').value = tc.method.toUpperCase();
                document.getElementById('headers').value = JSON.stringify({
                    "Content-Type": "application/json"
                }, null, 2);
                document.getElementById('body').value = JSON.stringify(tc.requestBody || {}, null, 2);
            });

            testCaseListDiv.appendChild(listItem);
            testCaseListDiv.appendChild(collapseDiv);
        });
    }

    // Function to escape HTML to prevent rendering in the browser
    function escapeHtml(text) {
        return text.replace(/&/g, "&amp;")
                   .replace(/</g, "&lt;")
                   .replace(/>/g, "&gt;")
                   .replace(/"/g, "&quot;")
                   .replace(/'/g, "&#039;");
    }
});
