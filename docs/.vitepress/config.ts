import {withMermaid} from "vitepress-plugin-mermaid";

export default withMermaid({
    title: 'FDE',
    description: 'FDE Documentation',
    base: process.env.DOCS_BASE || '',
    appearance: true,
    lastUpdated: true,
    themeConfig: {
        siteTitle: false,

        editLink: {
            pattern: 'https://github.com/medizininformatik-initiative/fhir-data-evaluator/edit/main/docs/:path',
            text: 'Edit this page on GitHub'
        },

        socialLinks: [
            {icon: 'github', link: 'https://github.com/medizininformatik-initiative/fhir-data-evaluator'}
        ],

        search: {
            provider: 'local'
        },

        outline: {
            level: [2, 3]
        },

        nav: [
            {text: 'Home', link: '/'}
        ],

        sidebar: [
            {
                text: 'Home',
                link: '/index.md'
            },
            {
                text: 'Overview',
                link: '/overview.md'
            },
            {
                text: 'Usage',
                link: '/usage.md'
            },
            {
                text: 'Configuration',
                link: '/configuration.md'
            },
            {
                text: 'Documentation',
                link: '/documentation.md'
            }
        ]
    }
})