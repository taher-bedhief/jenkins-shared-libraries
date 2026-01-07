#!/usr/bin/env groovy

/**
 * Shared Library: Update Kubernetes manifests with new image tags
 * Usage: updateK8sManifests(imageTag: '123', manifestsPath: 'kubernetes')
 */
def call(Map config = [:]) {
    // Paramètres
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

        // Update application deployment image
        sh """
            sed -i'' -e "s|image: taher2bedhief/echryapp:.*|image: taher2bedhief/echryapp:${imageTag}|g" ${manifestsPath}/08-echry-deployment.yaml

            # Update migration job image if it exists
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i'' -e "s|image: taher2bedhief/echrymigration:.*|image: taher2bedhief/echrymigration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi

            # Update ingress host if file exists
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i'' -e "s|host: .*|host: www.ech-ry.com|g" ${manifestsPath}/10-ingress.yaml
            fi

            # Commit changes if any
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
