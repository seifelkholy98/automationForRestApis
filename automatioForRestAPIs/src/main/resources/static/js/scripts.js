document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('file-upload-form');
    const spinner = document.getElementById('loading-spinner');
    const messageDiv = document.getElementById('message');
    const downloadLinkDiv = document.getElementById('download-link');
    const testCasesList = document.getElementById('test-cases-list');
    const uploadButton = form.querySelector('button[type="submit"]');

    // Initialize the spinner
    spinner.className = 'spinner-border text-primary';
    spinner.role = 'status';
    spinner.style.display = 'none';
    spinner.innerHTML = '<span class="visually-hidden">Loading...</span>';

    form.addEventListener('submit', function(event) {
        event.preventDefault();

        // Disable the upload button
        uploadButton.disabled = true;
        uploadButton.innerHTML = 'Uploading...'; // Update button text to indicate progress

        const formData = new FormData(this);
        spinner.style.display = 'block';
        messageDiv.innerText = '';
        downloadLinkDiv.innerHTML = '';
        testCasesList.innerHTML = '';  // Clear the current test cases

        fetch('/upload', {
            method: 'POST',
            body: formData,
        })
        .then(response => response.json())
        .then(data => {
            spinner.style.display = 'none';
            uploadButton.disabled = false;
            uploadButton.innerHTML = '<i class="fas fa-cloud-upload-alt"></i> Upload'; // Reset button text

            messageDiv.innerText = data.message;

            if (data.downloadLink) {
                downloadLinkDiv.innerHTML = `<a href="${data.downloadLink}" class="btn btn-success mt-3"><i class="fas fa-download"></i> Download Test Cases</a>`;
            }

            let testCases = [];
            if (data.testCases) {
                try {
                    if (Array.isArray(data.testCases)) {
                        testCases = data.testCases;
                    } else {
                        testCases = JSON.parse(data.testCases);
                    }

                    if (!Array.isArray(testCases)) {
                        throw new Error("Parsed testCases is not an array");
                    }
                } catch (error) {
                    messageDiv.innerText = 'Error parsing test cases: ' + error.message;
                    return;
                }

                // Save test cases to localStorage for use in the API Executor tab
                localStorage.setItem('generatedTestCases', JSON.stringify(testCases));

                displayTestCases(testCases);
                displayTestCaseMetrics(testCases);
            }
        })
        .catch(error => {
            spinner.style.display = 'none';
            uploadButton.disabled = false;
            uploadButton.innerHTML = '<i class="fas fa-cloud-upload-alt"></i> Upload'; // Reset button text
            messageDiv.innerText = 'Error: ' + error.message;
        });
    });

    function displayTestCases(testCases) {
        if (!Array.isArray(testCases)) {
            messageDiv.innerText = 'Test cases data is not an array';
            return;
        }

        testCasesList.innerHTML = '';

        testCases.forEach(tc => {
            const testCaseDiv = document.createElement('div');
            testCaseDiv.classList.add('test-case', 'col-md-12', 'mb-4');
            testCaseDiv.innerHTML = `
                <div class="card">
                    <div class="card-header">
                        <h4>${decodeURIComponent(tc.endpoint.replace(/\+/g, ' '))}</h4>
                        <p><strong>Method:</strong> ${decodeURIComponent(tc.method.replace(/\+/g, ' '))}</p>
                    </div>
                    <div class="card-body">
                        <p><strong>Categories:</strong> ${tc.categories ? tc.categories.map(cat => decodeURIComponent(cat.replace(/\+/g, ' '))).join(', ') : 'None'}</p>
                        <p><strong>Tags:</strong> ${tc.tags ? tc.tags.map(tag => decodeURIComponent(tag.replace(/\+/g, ' '))).join(', ') : 'None'}</p>
                        <hr>
                        <div class="test-case-details">
                            ${generateTestCaseDetails(tc.testCaseDetails)}
                        </div>
                    </div>
                </div>
            `;
            testCasesList.appendChild(testCaseDiv);
        });
    }

    function generateTestCaseDetails(details) {
        if (!Array.isArray(details)) {
            return `<p>Error: Test case details are not in the expected format.</p>`;
        }

        return details.map((detail, index) => `
            <div class="test-case-detail">
                <p><strong>Description:</strong> ${decodeURIComponent(detail.description.replace(/\+/g, ' '))}</p>
                <p><strong>Steps:</strong> ${detail.steps ? detail.steps.map(step => decodeURIComponent(step.replace(/\+/g, ' '))).join('<br>') : 'No steps provided'}</p>
                <p><strong>Keywords:</strong> ${detail.keywords ? detail.keywords.map(keyword => decodeURIComponent(keyword.replace(/\+/g, ' '))).join(', ') : 'None'}</p>
                <hr>
            </div>
        `).join('');
    }

    function filterTestCases() {
        const categoryValue = categoryFilter.value.toLowerCase();
        const tagValue = tagFilter.value.toLowerCase();

        const testCases = document.querySelectorAll('.test-case');
        testCases.forEach(tc => {
            const categories = tc.querySelector('.card-body > p:nth-of-type(1)').textContent.toLowerCase();
            const tags = tc.querySelector('.card-body > p:nth-of-type(2)').textContent.toLowerCase();

            if ((categoryValue === "" || categories.includes(categoryValue)) &&
                (tagValue === "" || tags.includes(tagValue))) {
                tc.style.display = 'block';
            } else {
                tc.style.display = 'none';
            }
        });
    }

    function displayTestCaseMetrics(testCases) {
        const categoryCounts = {};

        testCases.forEach(tc => {
            if (tc.categories) {
                tc.categories.forEach(cat => {
                    categoryCounts[cat] = (categoryCounts[cat] || 0) + 1;
                });
            }
        });

        const ctx = document.getElementById('testCaseChart').getContext('2d');
        new Chart(ctx, {
            type: 'pie',
            data: {
                labels: Object.keys(categoryCounts),
                datasets: [{
                    label: 'Test Cases by Category',
                    data: Object.values(categoryCounts),
                    backgroundColor: [
                        'rgba(75, 192, 192, 0.2)',
                        'rgba(255, 99, 132, 0.2)',
                        'rgba(54, 162, 235, 0.2)',
                        'rgba(255, 206, 86, 0.2)'
                    ],
                    borderColor: [
                        'rgba(75, 192, 192, 1)',
                        'rgba(255, 99, 132, 1)',
                        'rgba(54, 162, 235, 1)',
                        'rgba(255, 206, 86, 1)'
                    ],
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true
            }
        });
    }
});
