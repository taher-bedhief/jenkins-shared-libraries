#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags
 */
def call(Map config = [:]) {
    def imageTag       = config.imageTag     ?: error("Image tag is required")
    def manifestsPath  = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'crd_github'
    def gitUserName    = config.gitUserName  ?: 'Jenkins CI'
    def gitUserEmail   = config.gitUserEmail ?: 'jenkins@example.com'
    def gitBranch      = config.gitBranch    ?: 'main'
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Configure Git local
        sh """
            git config --local user.name "${gitUserName}"
            git config --local user.email "${gitUserEmail}"
        """
        
        // Update echry deployment
        sh """
            sed -i "s|image: taher2bedhief/echry-app:.*|image: taher2bedhief/echry-app:${imageTag}|g" ${manifestsPath}/08-echry-deployment.yaml

            # Update migration job if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: taher2bedhief/echry-migration:.*|image: taher2bedhief/echry-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Ensure ingress is using the correct domain
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: www.ech-ry.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Check for changes
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                
                git remote set-url origin https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/taher-bedhief/ech-ry-site.git
                git push origin HEAD:${gitBranch}
            fi
        """
    }
}













































