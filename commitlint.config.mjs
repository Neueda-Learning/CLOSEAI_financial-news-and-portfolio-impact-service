export default {
  extends: ['@commitlint/config-conventional'],
  ignores: [
    (message) => message.startsWith('Merge '),
    (message) => message.startsWith('Restore 12-slide Swiss HTML presentation'),
  ],
  rules: {
    'type-enum': [
      2,
      'always',
      ['feat', 'fix', 'docs', 'refactor', 'test', 'chore', 'ci'],
    ],
    'body-max-line-length': [0],
    'subject-case': [0],
  },
};
