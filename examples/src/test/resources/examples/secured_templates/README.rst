The configuration in this example has two template.  One that is secured and only a user which has ROLE_USER can access it and it
has a second template that is public.  When doing a get capabilities with no authentication only the unsecured template will be
listed.  If you use basic auth with credentials jimi/jimi or bob/bob then both templates will be accessible and listed.